/*
 * APIBalance 备份升级工具（纯浏览器端）
 * Copyright (C) 2026 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * 这个文件只做一件事：把旧版备份包解出来，用新版参数重新加密成 v3 备份包。
 * 所有运算都在浏览器的 WebCrypto 里完成，没有任何网络请求，没有上传接口。
 *
 * 支持的输入：
 *   v1  旧 CBC 格式：salt(16) || iv(16) || AES-CBC/PKCS5(明文)
 *   v2  过渡 GCM 格式：RXHBK2(6) || salt(16) || iv(12) || AES-GCM(明文)
 *   外层还允许套一层 zip（内含 backup.enc），App 导出 .zip 时就是这样
 *
 * 输出：
 *   v3  当前格式：RXHBK3(6) || salt(16) || nonce(12) || AES-GCM(明文)
 *       -- PBKDF2-HMAC-SHA256 600000 次，256 位密钥
 *       -- 12 字节随机 nonce（每次重新生成，绝不复用）
 *       -- GCM tag 128 位
 *       -- AAD = magic || version || kdfId || 迭代次数(4,大端) || salt(16)
 *       明文仍然是 zip || HMAC-SHA256(32) || "APIBAL01"，与 App 的验签逻辑一致
 */
(function (global) {
  'use strict';

  // 官方签名指纹，App 里 EnvChecker.OFFICIAL_CERT_SHA256 同一个常量（构建文档也已公开）
  var CERT_SHA256 = '08:8E:E5:FA:C5:2E:7F:9A:8B:89:7D:6E:D9:E9:9E:E2:22:B1:F8:46:18:4D:74:19:22:DE:A6:6D:DA:BB:92:9A';
  var SIGN_SALT = 'APIBalance-backup-sign-v2';
  var SIGN_ITER = 10000;
  var SIGN_PREFIX = 'APIBalance/';

  var V2_MAGIC = 'RXHBK2';
  var V3_MAGIC = 'RXHBK3';
  var SIG_MAGIC = 'APIBAL01';

  var V2_ITER = 210000;            // 旧 v2 的迭代次数，只能照原样读
  var V3_ITER = 600000;            // v3 新参数
  var V3_KDF_ID = 1;               // 1 = PBKDF2-HMAC-SHA256
  var V3_VERSION = 3;
  var TAG_BITS = 128;

  function utf8(s) {
    return new TextEncoder().encode(s);
  }

  function concat() {
    var total = 0, i, a;
    for (i = 0; i < arguments.length; i++) total += arguments[i].length;
    var out = new Uint8Array(total);
    var off = 0;
    for (i = 0; i < arguments.length; i++) {
      a = arguments[i];
      out.set(a, off);
      off += a.length;
    }
    return out;
  }

  function startsWith(bytes, ascii) {
    if (!bytes || bytes.length < ascii.length) return false;
    for (var i = 0; i < ascii.length; i++) {
      if (bytes[i] !== ascii.charCodeAt(i)) return false;
    }
    return true;
  }

  function endsWith(bytes, ascii) {
    if (!bytes || bytes.length < ascii.length) return false;
    var off = bytes.length - ascii.length;
    for (var i = 0; i < ascii.length; i++) {
      if (bytes[off + i] !== ascii.charCodeAt(i)) return false;
    }
    return true;
  }

  function writeIteration(v) {
    return new Uint8Array([(v >>> 24) & 0xff, (v >>> 16) & 0xff, (v >>> 8) & 0xff, v & 0xff]);
  }

  /** AAD 绑定格式元数据：换版本、换 KDF、换迭代次数、换 salt 都会让解密直接失败。 */
  function aadFor(salt) {
    return concat(utf8(V3_MAGIC), new Uint8Array([V3_VERSION, V3_KDF_ID]),
      writeIteration(V3_ITER), salt);
  }

  function subtle() {
    var c = global.crypto || (global.msCrypto);
    if (!c || !c.subtle) throw new Error('这个浏览器不支持 WebCrypto，没法在本地解密');
    return c.subtle;
  }

  function randomBytes(n) {
    var b = new Uint8Array(n);
    var c = global.crypto;
    if (!c || !c.getRandomValues) throw new Error('这个浏览器不支持安全随机数');
    c.getRandomValues(b);
    return b;
  }

  async function pbkdf2(password, salt, iterations, hash, bits) {
    var key = await subtle().importKey('raw', utf8(password), 'PBKDF2', false, ['deriveBits']);
    var buf = await subtle().deriveBits(
      { name: 'PBKDF2', salt: salt, iterations: iterations, hash: hash }, key, bits);
    return new Uint8Array(buf);
  }

  async function aesKey(raw) {
    return subtle().importKey('raw', raw, { name: 'AES-GCM' }, false, ['encrypt', 'decrypt']);
  }

  // ---------- 签名（和 App 完全一致） ----------
  async function signKey(password) {
    var pwdKey = await pbkdf2(password, utf8(SIGN_SALT), SIGN_ITER, 'SHA-256', 256);
    var macKey = await subtle().importKey('raw', pwdKey, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
    var derived = await subtle().sign('HMAC', macKey, utf8(SIGN_PREFIX + CERT_SHA256));
    return subtle().importKey('raw', new Uint8Array(derived), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  }

  async function hmacTag(body, password) {
    var key = await signKey(password);
    var tag = await subtle().sign('HMAC', key, body);
    return new Uint8Array(tag);
  }

  // ---------- 外层 zip（只取 backup.enc 一个条目） ----------
  async function unzipSingle(bytes) {
    // EOCD 签名 0x06054b50，从尾部往前找
    var eocd = -1;
    for (var i = bytes.length - 22; i >= 0 && i >= bytes.length - 22 - 65535; i--) {
      if (bytes[i] === 0x50 && bytes[i + 1] === 0x4b && bytes[i + 2] === 0x05 && bytes[i + 3] === 0x06) {
        eocd = i;
        break;
      }
    }
    if (eocd < 0) throw new Error('zip 结构不认识');
    var dv = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    var entries = dv.getUint16(eocd + 10, true);
    var cdSize = dv.getUint32(eocd + 12, true);
    var cdOff = dv.getUint32(eocd + 16, true);
    if (entries < 1) throw new Error('zip 是空的');
    // 只处理第一条（App 只写 backup.enc）
    if (dv.getUint32(cdOff, true) !== 0x02014b50) throw new Error('zip 目录结构不认识');
    var method = dv.getUint16(cdOff + 10, true);
    var csize = dv.getUint32(cdOff + 20, true);
    var usize = dv.getUint32(cdOff + 24, true);
    var nameLen = dv.getUint16(cdOff + 28, true);
    var extraLen = dv.getUint16(cdOff + 30, true);
    var commentLen = dv.getUint16(cdOff + 32, true);
    var localOff = dv.getUint32(cdOff + 42, true);
    var name = new TextDecoder().decode(bytes.subarray(cdOff + 46, cdOff + 46 + nameLen));
    if (name !== 'backup.enc') throw new Error('zip 里没有 backup.enc');
    if (dv.getUint32(localOff, true) !== 0x04034b50) throw new Error('zip 本地头损坏');
    var lNameLen = dv.getUint16(localOff + 26, true);
    var lExtraLen = dv.getUint16(localOff + 28, true);
    var dataOff = localOff + 30 + lNameLen + lExtraLen;
    var raw = bytes.subarray(dataOff, dataOff + csize);
    if (method === 0) return new Uint8Array(raw);
    if (method !== 8) throw new Error('zip 用了不支持的压缩方式');
    if (typeof DecompressionStream === 'undefined') {
      throw new Error('这个浏览器不支持本地解压 zip，请把文件解压后用里面的 backup.enc');
    }
    var stream = new DecompressionStream('deflate-raw');
    var blob = new Response(new Blob([raw]).stream().pipeThrough(stream));
    var out = new Uint8Array(await blob.arrayBuffer());
    if (usize && out.length !== usize) throw new Error('zip 解压长度不对');
    return out;
  }

  // ---------- 三种格式的解密 ----------
  async function decryptV1(blob, password) {
    if (blob.length < 33) throw new Error('文件太短');
    var salt = blob.subarray(0, 16);
    var iv = blob.subarray(16, 32);
    var enc = blob.subarray(32);
    var raw = await pbkdf2(password, salt, 2000, 'SHA-1', 256); // 旧格式只能是 SHA-1 2000 次
    var key = await subtle().importKey('raw', raw, { name: 'AES-CBC' }, false, ['decrypt']);
    var plain = new Uint8Array(await subtle().decrypt({ name: 'AES-CBC', iv: iv }, key, enc));
    return plain;
  }

  async function decryptV2(blob, password) {
    var off = V2_MAGIC.length;
    if (blob.length < off + 16 + 12 + 16) throw new Error('文件太短');
    var salt = blob.subarray(off, off + 16);
    var iv = blob.subarray(off + 16, off + 28);
    var enc = blob.subarray(off + 28);
    var raw = await pbkdf2(password, salt, V2_ITER, 'SHA-256', 256);
    var key = await aesKey(raw);
    var plain = new Uint8Array(await subtle().decrypt(
      { name: 'AES-GCM', iv: iv, tagLength: TAG_BITS }, key, enc));
    return plain;
  }

  async function decryptV3(blob, password) {
    var off = V3_MAGIC.length;
    if (blob.length < off + 16 + 12 + 16) throw new Error('文件太短');
    var salt = blob.subarray(off, off + 16);
    var nonce = blob.subarray(off + 16, off + 28);
    var enc = blob.subarray(off + 28);
    var raw = await pbkdf2(password, salt, V3_ITER, 'SHA-256', 256);
    var key = await aesKey(raw);
    var plain = new Uint8Array(await subtle().decrypt(
      { name: 'AES-GCM', iv: nonce, additionalData: aadFor(salt), tagLength: TAG_BITS }, key, enc));
    return plain;
  }

  async function encryptV3(plain, password) {
    var salt = randomBytes(16);
    var nonce = randomBytes(12); // 每次都新取，绝不复用
    var raw = await pbkdf2(password, salt, V3_ITER, 'SHA-256', 256);
    var key = await aesKey(raw);
    var enc = new Uint8Array(await subtle().encrypt(
      { name: 'AES-GCM', iv: nonce, additionalData: aadFor(salt), tagLength: TAG_BITS }, key, plain));
    return concat(utf8(V3_MAGIC), salt, nonce, enc);
  }

  // ---------- 对外流程 ----------
  function sniff(blob) {
    if (blob.length > 4 && blob[0] === 0x50 && blob[1] === 0x4b) return 'zip';
    if (startsWith(blob, V3_MAGIC)) return 'v3';
    if (startsWith(blob, V2_MAGIC)) return 'v2';
    return 'v1';
  }

  /** 校验明文尾部的 HMAC + 标记；顺带证明口令是对的（旧格式没有 AEAD tag，只能靠它）。 */
  async function verifyAndStrip(plain, password) {
    var sigLen = SIG_MAGIC.length;
    var tail = 32 + sigLen;
    if (plain.length <= tail + 64) throw new Error('文件结构不完整');
    if (!endsWith(plain, SIG_MAGIC)) throw new Error('没有签名标记，不是本应用的备份');
    var body = plain.subarray(0, plain.length - tail);
    var expect = await hmacTag(body, password);
    var got = plain.subarray(plain.length - tail, plain.length - tail + 32);
    var diff = 0;
    for (var i = 0; i < 32; i++) diff |= expect[i] ^ got[i];
    if (diff !== 0) throw new Error('口令错误或文件被修改过');
    return { body: body, signed: plain };
  }

  async function convert(inputBytes, password) {
    if (!password || password.length < 4) throw new Error('口令太短');
    var blob = inputBytes;
    var wrapper = 'bin';
    if (sniff(blob) === 'zip') {
      blob = await unzipSingle(blob);
      wrapper = 'zip';
    }
    var from = sniff(blob);
    if (from === 'zip') throw new Error('zip 里不是备份数据');

    var plain;
    try {
      if (from === 'v1') plain = await decryptV1(blob, password);
      else if (from === 'v2') plain = await decryptV2(blob, password);
      else plain = await decryptV3(blob, password);
    } catch (e) {
      // 解密层失败一律归成一句话，不区分“填充错”还是“tag 错”，避免泄露细节
      throw new Error('口令错误或文件损坏');
    }

    var checked = await verifyAndStrip(plain, password);
    var out = await encryptV3(checked.signed, password);
    return {
      from: from,
      fromName: { v1: '旧版 CBC 格式（v1）', v2: '过渡 GCM 格式（v2）', v3: '当前格式（v3）' }[from],
      wrapper: wrapper,
      bytes: out,
      plainBytes: checked.body.length
    };
  }

  /** 目前页面只做“旧 → 新”，v3 输入会原样提示并跳过。 */
  async function convertIfNeeded(inputBytes, password) {
    var res = await convert(inputBytes, password);
    if (res.from === 'v3') return { converted: false, result: res };
    return { converted: true, result: res };
  }

  var api = {
    convert: convert,
    convertIfNeeded: convertIfNeeded,
    sniff: sniff,
    aadFor: aadFor,
    constants: {
      V3_ITER: V3_ITER, V2_ITER: V2_ITER, V3_KDF_ID: V3_KDF_ID, V3_VERSION: V3_VERSION,
      V3_MAGIC: V3_MAGIC, V2_MAGIC: V2_MAGIC, SIG_MAGIC: SIG_MAGIC, CERT_SHA256: CERT_SHA256
    }
  };

  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else global.BackupConvert = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
