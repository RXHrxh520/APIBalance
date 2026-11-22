package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TokenEstimatorTest {

    @Test
    public void emptyTextIsZero() {
        assertEquals(0, TokenEstimator.estimate(""));
        assertEquals(0, TokenEstimator.estimate(null));
    }

    @Test
    public void asciiCountsOnePerCharacter() {
        assertEquals(3, TokenEstimator.estimate("abc"));
    }

    @Test
    public void cjkUsesOneAndHalfTokens() {
        assertEquals(3, TokenEstimator.estimate("你好"));
    }

    @Test
    public void mixedTextAddsBothParts() {
        assertEquals(3, TokenEstimator.estimate("a你"));
    }
}
