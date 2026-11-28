/*
 * 备份升级工具的本地测试（Node 端跑，不联网、不依赖 Android）
 * Copyright (C) 2026 软小寒
 *
 * 用法：node scripts/tests/backup_convert.test.mjs
 *
 * 覆盖：v1（CBC）→ v3、v2（GCM）→ v3、外层 zip 包装、错误口令拒绝、
 * nonce 不复用、AAD 绑定生效，以及签名字节完全一致（改完仍能被 App 验签）。
 */
import { createRequire } from 'node:module';
import { pbkdf2Sync, randomBytes, createHmac, createCipheriv, createDecipheriv } from 'node:crypto';
import { deflateRawSync } from 'node:zlib';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import path from 'node:path';

const require = createRequire(import.meta.url);
const root = path.resolve(import.meta.dirname, '../..');
const conv = require(path.join(root, 'docs/assets/js/backup-convert.js'));

const CERT = conv.constants.CERT_SHA256;
const SIG = conv.constants.SIG_MAGIC;
const PWD = 'RxH-test-2026';

let failed = 0;
function ok(cond, name) {
  if (cond) console.log('  ✓ ' + name);
  else { console.log('  ✗ ' + name); failed++; }
}

function utf8(s) { return Buffer.from(s, 'utf8'); }

function signKey(password) {
  const pwdKey = pbkdf2Sync(utf8(password), utf8('APIBalance-backup-sign-v2'), 10000, 32, 'sha256');
  return createHmac('sha256', pwdKey).update(utf8('APIBalance/' + CERT)).digest();
}

function signed(zip, password) {
  const tag = createHmac('sha256', signKey(password)).update(zip).digest();
  return Buffer.concat([zip, tag, utf8(SIG)]);
}

/** 最小 zip 写入器，只写一个条目 */
function zipOne(name, data, deflate) {
  const nameBuf = utf8(name);
  const payload = deflate ? deflateRawSync(data) : data;
  const method = deflate ? 8 : 0;
  const crc = crc32(data);
  const local = Buffer.alloc(30);
  local.writeUInt32LE(0x04034b50, 0);
  local.writeUInt16LE(20, 4);
  local.writeUInt16LE(0, 6);
  local.writeUInt16LE(method, 8);
  local.writeUInt32LE(0, 10);
  local.writeUInt32LE(crc, 14);
  local.writeUInt32LE(payload.length, 18);
  local.writeUInt32LE(data.length, 22);
  local.writeUInt16LE(nameBuf.length, 26);
  local.writeUInt16LE(0, 28);
  const body = Buffer.concat([local, nameBuf, payload]);
  const cd = Buffer.alloc(46);
  cd.writeUInt32LE(0x02014b50, 0);
  cd.writeUInt16LE(20, 4);
  cd.writeUInt16LE(20, 6);
  cd.writeUInt16LE(0, 8);
  cd.writeUInt16LE(method, 10);
  cd.writeUInt32LE(0, 12);
  cd.writeUInt32LE(crc, 16);
  cd.writeUInt32LE(payload.length, 20);
  cd.writeUInt32LE(data.length, 24);
  cd.writeUInt16LE(nameBuf.length, 28);
  cd.writeUInt16LE(0, 30);
  cd.writeUInt16LE(0, 32);
  cd.writeUInt16LE(0, 34);
  cd.writeUInt16LE(0, 36);
  cd.writeUInt32LE(0, 38);
  cd.writeUInt32LE(0, 42);
  const cdBuf = Buffer.concat([cd, nameBuf]);
  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(0x06054b50, 0);
  eocd.writeUInt16LE(1, 8);
  eocd.writeUInt16LE(1, 10);
  eocd.writeUInt32LE(cdBuf.length, 12);
  eocd.writeUInt32LE(body.length, 16);
  return Buffer.concat([body, cdBuf, eocd]);
}

const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let i = 0; i < 256; i++) {
    let c = i;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[i] = c >>> 0;
  }
  return t;
})();
function crc32(buf) {
  let c = 0xffffffff;
  for (const b of buf) c = CRC_TABLE[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function v1Blob(plain, password) {
  const salt = randomBytes(16);
  const iv = randomBytes(16);
  const key = pbkdf2Sync(utf8(password), salt, 2000, 32, 'sha1');
  const c = createCipheriv('aes-256-cbc', key, iv);
  const enc = Buffer.concat([c.update(plain), c.final()]);
  return Buffer.concat([salt, iv, enc]);
}

function v2Blob(plain, password) {
  const salt = randomBytes(16);
  const iv = randomBytes(12);
  const key = pbkdf2Sync(utf8(password), salt, conv.constants.V2_ITER, 32, 'sha256');
  const c = createCipheriv('aes-256-gcm', key, iv);
  const enc = Buffer.concat([c.update(plain), c.final(), c.getAuthTag()]);
  return Buffer.concat([utf8('RXHBK2'), salt, iv, enc]);
}

/** 独立实现一遍 v3 解密，不借用被测代码的 encrypt，防止自证 */
function openV3(blob, password) {
  const salt = blob.subarray(6, 22);
  const nonce = blob.subarray(22, 34);
  const tag = blob.subarray(blob.length - 16);
  const ct = blob.subarray(34, blob.length - 16);
  const iter = conv.constants.V3_ITER;
  const aad = Buffer.concat([
    utf8('RXHBK3'), Buffer.from([conv.constants.V3_VERSION, conv.constants.V3_KDF_ID]),
    Buffer.from([(iter >>> 24) & 0xff, (iter >>> 16) & 0xff, (iter >>> 8) & 0xff, iter & 0xff]),
    salt
  ]);
  const key = pbkdf2Sync(utf8(password), salt, iter, 32, 'sha256');
  const d = createDecipheriv('aes-256-gcm', key, nonce);
  d.setAAD(aad);
  d.setAuthTag(tag);
  return Buffer.concat([d.update(ct), d.final()]);
}

function sha(b) { return createHash('sha256').update(b).digest('hex'); }

async function main() {
  console.log('备份升级工具 · 本地互操作测试');

  const files = {
    'databases/balance.db': Buffer.from('SQLite format 3\u0000 假装这是数据库 '.repeat(40)),
    'keys.txt': Buffer.from('A\t1\tZGVlcHNlZWs=\tZ2hwX3Rlc3Q=\n', 'utf8').toString('base64')
  };
  const rawZip = zipOne('payload.zip.txt', Buffer.from('fake-inner-zip '.repeat(50)), true);
  const plainBody = signed(rawZip, PWD);

  // v1：没有外层 zip
  const legacy = v1Blob(plainBody, PWD);
  console.log('\n[v1 → v3]');
  const r1 = await conv.convert(legacy, PWD);
  ok(r1.from === 'v1', '识别为 v1');
  ok(sha(Buffer.from(r1.bytes)) && Buffer.from(openV3(r1.bytes, PWD)).equals(plainBody), 'v3 可被独立实现解回原文（含签名）');
  ok(r1.bytes[0] === 0x52 && r1.bytes.length > 6, '输出带 RXHBK3 头');

  // v2 + 外层 zip（deflate）
  const v2 = v2Blob(plainBody, PWD);
  const wrapped = zipOne('backup.enc', v2, true);
  console.log('\n[v2 + zip → v3]');
  const r2 = await conv.convert(wrapped, PWD);
  ok(r2.from === 'v2' && r2.wrapper === 'zip', '识别为 zip 里的 v2');
  ok(Buffer.from(openV3(r2.bytes, PWD)).equals(plainBody), '内容一致');

  // 口令错
  console.log('\n[负向用例]');
  let thrown = null;
  try { await conv.convert(legacy, 'wrong-pass'); } catch (e) { thrown = e; }
  ok(!!thrown && /口令/.test(thrown.message), '错误口令被拒绝（v1 靠 HMAC 兜底）');

  thrown = null;
  try { await conv.convert(v2, 'wrong-pass'); } catch (e) { thrown = e; }
  ok(!!thrown, '错误口令被拒绝（v2 靠 GCM tag）');

  // AAD 绑定：改一位 salt，解不开
  const tampered = Buffer.from(r1.bytes);
  tampered[7] ^= 0x01;
  thrown = null;
  try { await openV3(tampered, PWD); } catch (e) { thrown = e; }
  ok(!!thrown, 'salt 被改 → AAD/GCM 立刻失败');

  // nonce 不复用
  const a = await conv.convert(legacy, PWD);
  const b = await conv.convert(legacy, PWD);
  ok(Buffer.from(a.bytes.subarray(22, 34)).equals(Buffer.from(b.bytes.subarray(22, 34))) === false, '两次转换 nonce 不同');

  // 同一口令下 salt 也不同
  ok(Buffer.from(a.bytes.subarray(6, 22)).equals(Buffer.from(b.bytes.subarray(6, 22))) === false, '两次转换 salt 不同');

  // 落盘给 Java 交叉验证
  mkdirSync(path.join(root, '.work/tmp'), { recursive: true });
  writeFileSync(path.join(root, '.work/tmp/converted_v3.bin'), r1.bytes);
  writeFileSync(path.join(root, '.work/tmp/expected_plain.bin'), plainBody);
  writeFileSync(path.join(root, '.work/tmp/sample_v1.bin'), legacy);

  console.log('\n结果：' + (failed === 0 ? '全部通过' : failed + ' 项失败'));
  process.exit(failed === 0 ? 0 : 1);
}

main().catch((e) => { console.error(e); process.exit(1); });
