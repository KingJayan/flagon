/*
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunSoft, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice
 * is preserved.
 * ====================================================
 */
package org.teavm.runtime.wasi;

/**
 * flagon: fdlibm 5.3 in java, which a wasi build has in place of the page's Math. it gives the
 * same bits as StrictMath, which is fdlibm too.
 */
public final class WasiMath {
    private static final double ONE = 1.0;
    private static final double ZERO = 0.0;
    private static final double HALF = 0.5;
    private static final double HUGE = 1.0e300;
    private static final double TINY = 1.0e-300;
    private static final double TWO24 = 1.67772160000000000000e+07;
    private static final double TWON24 = 5.96046447753906250000e-08;
    private static final double TWO54 = 1.80143985094819840000e+16;
    private static final double TWOM54 = 5.55111512312578270212e-17;

    private WasiMath() {
    }

    private static int hi(double x) {
        return (int) (Double.doubleToRawLongBits(x) >>> 32);
    }

    private static int lo(double x) {
        return (int) Double.doubleToRawLongBits(x);
    }

    private static double words(int high, int low) {
        return Double.longBitsToDouble(((long) high << 32) | (low & 0xFFFFFFFFL));
    }

    private static double withHigh(double x, int high) {
        return words(high, lo(x));
    }

    private static double withLow(double x, int low) {
        return words(hi(x), low);
    }

    private static double abs(double x) {
        return Double.longBitsToDouble(Double.doubleToRawLongBits(x) & Long.MAX_VALUE);
    }

    private static double copySign(double magnitude, double sign) {
        return Double.longBitsToDouble((Double.doubleToRawLongBits(sign) & Long.MIN_VALUE)
                | (Double.doubleToRawLongBits(magnitude) & Long.MAX_VALUE));
    }

    public static double floor(double x) {
        if (!(abs(x) < 4503599627370496.0)) {
            return x;
        }
        double down = (double) (long) x;
        if (down > x) {
            down -= 1.0;
        }
        return down == 0.0 ? copySign(down, x) : down;
    }

    public static double ceil(double x) {
        return -floor(-x);
    }

    static double scalbn(double x, int n) {
        int hx = hi(x);
        int lx = lo(x);
        int k = (hx & 0x7ff00000) >> 20;
        if (k == 0) {
            if ((lx | (hx & 0x7fffffff)) == 0) {
                return x;
            }
            x *= TWO54;
            hx = hi(x);
            k = ((hx & 0x7ff00000) >> 20) - 54;
            if (n < -50000) {
                return TINY * x;
            }
        }
        if (k == 0x7ff) {
            return x + x;
        }
        k = k + n;
        if (k > 0x7fe) {
            return HUGE * copySign(HUGE, x);
        }
        if (k > 0) {
            return withHigh(x, (hx & 0x800fffff) | (k << 20));
        }
        if (k <= -54) {
            if (n > 50000) {
                return HUGE * copySign(HUGE, x);
            }
            return TINY * copySign(TINY, x);
        }
        k += 54;
        x = withHigh(x, (hx & 0x800fffff) | (k << 20));
        return x * TWOM54;
    }

    public static double sqrt(double x) {
        final int sign = 0x80000000;
        int ix0 = hi(x);
        int ix1 = lo(x);
        if ((ix0 & 0x7ff00000) == 0x7ff00000) {
            return x * x + x;
        }
        if (ix0 <= 0) {
            if (((ix0 & ~sign) | ix1) == 0) {
                return x;
            } else if (ix0 < 0) {
                return (x - x) / (x - x);
            }
        }
        int m = ix0 >> 20;
        if (m == 0) {
            while (ix0 == 0) {
                m -= 21;
                ix0 |= ix1 >>> 11;
                ix1 <<= 21;
            }
            int i = 0;
            while ((ix0 & 0x00100000) == 0) {
                ix0 <<= 1;
                i++;
            }
            m -= i - 1;
            if (i > 0) {
                ix0 |= ix1 >>> (32 - i);
            }
            ix1 <<= i;
        }
        m -= 1023;
        ix0 = (ix0 & 0x000fffff) | 0x00100000;
        if ((m & 1) != 0) {
            ix0 += ix0 + ((ix1 & sign) >>> 31);
            ix1 += ix1;
        }
        m >>= 1;
        ix0 += ix0 + ((ix1 & sign) >>> 31);
        ix1 += ix1;
        int q = 0;
        int q1 = 0;
        int s0 = 0;
        int s1 = 0;
        int r = 0x00200000;
        while (r != 0) {
            int t = s0 + r;
            if (t <= ix0) {
                s0 = t + r;
                ix0 -= t;
                q += r;
            }
            ix0 += ix0 + ((ix1 & sign) >>> 31);
            ix1 += ix1;
            r >>= 1;
        }
        r = sign;
        while (r != 0) {
            int t1 = s1 + r;
            int t = s0;
            if (t < ix0 || (t == ix0 && Integer.compareUnsigned(t1, ix1) <= 0)) {
                s1 = t1 + r;
                if ((t1 & sign) == sign && (s1 & sign) == 0) {
                    s0 += 1;
                }
                ix0 -= t;
                if (Integer.compareUnsigned(ix1, t1) < 0) {
                    ix0 -= 1;
                }
                ix1 -= t1;
                q1 += r;
            }
            ix0 += ix0 + ((ix1 & sign) >>> 31);
            ix1 += ix1;
            r >>>= 1;
        }
        if ((ix0 | ix1) != 0) {
            if (q1 == 0xffffffff) {
                q1 = 0;
                q += 1;
            } else {
                q1 += q1 & 1;
            }
        }
        ix0 = (q >> 1) + 0x3fe00000;
        ix1 = q1 >>> 1;
        if ((q & 1) == 1) {
            ix1 |= sign;
        }
        ix0 += m << 20;
        return words(ix0, ix1);
    }

    // sin, cos and tan of an argument reduced to [-pi/4, pi/4]

    private static final double S1 = -1.66666666666666324348e-01;
    private static final double S2 = 8.33333333332248946124e-03;
    private static final double S3 = -1.98412698298579493134e-04;
    private static final double S4 = 2.75573137070700676789e-06;
    private static final double S5 = -2.50507602534068634195e-08;
    private static final double S6 = 1.58969099521155010221e-10;

    private static double kernelSin(double x, double y, int iy) {
        int ix = hi(x) & 0x7fffffff;
        if (ix < 0x3e400000) {
            if ((int) x == 0) {
                return x;
            }
        }
        double z = x * x;
        double v = z * x;
        double r = S2 + z * (S3 + z * (S4 + z * (S5 + z * S6)));
        if (iy == 0) {
            return x + v * (S1 + z * r);
        }
        return x - ((z * (HALF * y - v * r) - y) - v * S1);
    }

    private static final double C1 = 4.16666666666666019037e-02;
    private static final double C2 = -1.38888888888741095749e-03;
    private static final double C3 = 2.48015872894767294178e-05;
    private static final double C4 = -2.75573143513906633035e-07;
    private static final double C5 = 2.08757232129817482790e-09;
    private static final double C6 = -1.13596475577881948265e-11;

    private static double kernelCos(double x, double y) {
        int ix = hi(x) & 0x7fffffff;
        if (ix < 0x3e400000) {
            if ((int) x == 0) {
                return ONE;
            }
        }
        double z = x * x;
        double r = z * (C1 + z * (C2 + z * (C3 + z * (C4 + z * (C5 + z * C6)))));
        if (ix < 0x3FD33333) {
            return ONE - (0.5 * z - (z * r - x * y));
        }
        double qx = ix > 0x3fe90000 ? 0.28125 : words(ix - 0x00200000, 0);
        double hz = 0.5 * z - qx;
        double a = ONE - qx;
        return a - (hz - (z * r - x * y));
    }

    private static final double[] T = {
        3.33333333333334091986e-01,
        1.33333333333201242699e-01,
        5.39682539762260521377e-02,
        2.18694882948595424599e-02,
        8.86323982359930005737e-03,
        3.59207910759131235356e-03,
        1.45620945432529025516e-03,
        5.88041240820264096874e-04,
        2.46463134818469906812e-04,
        7.81794442939557092300e-05,
        7.14072491382608190305e-05,
        -1.85586374855275456654e-05,
        2.59073051863633712884e-05,
    };
    private static final double PIO4 = 7.85398163397448278999e-01;
    private static final double PIO4LO = 3.06161699786838301793e-17;

    private static double kernelTan(double x, double y, int iy) {
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        double z;
        double w;
        if (ix < 0x3e300000) {
            if ((int) x == 0) {
                if (((ix | lo(x)) | (iy + 1)) == 0) {
                    return ONE / abs(x);
                } else if (iy == 1) {
                    return x;
                } else {
                    w = x + y;
                    z = withLow(w, 0);
                    double v = y - (z - x);
                    double a = -ONE / w;
                    double t = withLow(a, 0);
                    double s = ONE + t * z;
                    return t + a * (s + t * v);
                }
            }
        }
        if (ix >= 0x3FE59428) {
            if (hx < 0) {
                x = -x;
                y = -y;
            }
            z = PIO4 - x;
            w = PIO4LO - y;
            x = z + w;
            y = 0.0;
        }
        z = x * x;
        w = z * z;
        double r = T[1] + w * (T[3] + w * (T[5] + w * (T[7] + w * (T[9] + w * T[11]))));
        double v = z * (T[2] + w * (T[4] + w * (T[6] + w * (T[8] + w * (T[10] + w * T[12])))));
        double s = z * x;
        r = y + z * (s * (r + v) + y);
        r += T[0] * s;
        w = x + r;
        if (ix >= 0x3FE59428) {
            v = iy;
            return (1 - ((hx >> 30) & 2)) * (v - 2.0 * (x - (w * w / (w + v) - r)));
        }
        if (iy == 1) {
            return w;
        }
        z = withLow(w, 0);
        v = r - (z - x);
        double a = -1.0 / w;
        double t = withLow(a, 0);
        s = 1.0 + t * z;
        return t + a * (s + t * v);
    }

    // argument reduction by pi/2

    private static final int[] NPIO2_HW = {
        0x3FF921FB, 0x400921FB, 0x4012D97C, 0x401921FB, 0x401F6A7A, 0x4022D97C,
        0x4025FDBB, 0x402921FB, 0x402C463A, 0x402F6A7A, 0x4031475C, 0x4032D97C,
        0x40346B9C, 0x4035FDBB, 0x40378FDB, 0x403921FB, 0x403AB41B, 0x403C463A,
        0x403DD85A, 0x403F6A7A, 0x40407E4C, 0x4041475C, 0x4042106C, 0x4042D97C,
        0x4043A28C, 0x40446B9C, 0x404534AC, 0x4045FDBB, 0x4046C6CB, 0x40478FDB,
        0x404858EB, 0x404921FB,
    };
    private static final int[] TWO_OVER_PI = {
        0xA2F983, 0x6E4E44, 0x1529FC, 0x2757D1, 0xF534DD, 0xC0DB62,
        0x95993C, 0x439041, 0xFE5163, 0xABDEBB, 0xC561B7, 0x246E3A,
        0x424DD2, 0xE00649, 0x2EEA09, 0xD1921C, 0xFE1DEB, 0x1CB129,
        0xA73EE8, 0x8235F5, 0x2EBB44, 0x84E99C, 0x7026B4, 0x5F7E41,
        0x3991D6, 0x398353, 0x39F49C, 0x845F8B, 0xBDF928, 0x3B1FF8,
        0x97FFDE, 0x05980F, 0xEF2F11, 0x8B5A0A, 0x6D1F6D, 0x367ECF,
        0x27CB09, 0xB74F46, 0x3F669E, 0x5FEA2D, 0x7527BA, 0xC7EBE5,
        0xF17B3D, 0x0739F7, 0x8A5292, 0xEA6BFB, 0x5FB11F, 0x8D5D08,
        0x560330, 0x46FC7B, 0x6BABF0, 0xCFBC20, 0x9AF436, 0x1DA9E3,
        0x91615E, 0xE61B08, 0x659985, 0x5F14A0, 0x68408D, 0xFFD880,
        0x4D7327, 0x310606, 0x1556CA, 0x73A8C9, 0x60E27B, 0xC08C6B,
    };
    private static final double INVPIO2 = 6.36619772367581382433e-01;
    private static final double PIO2_1 = 1.57079632673412561417e+00;
    private static final double PIO2_1T = 6.07710050650619224932e-11;
    private static final double PIO2_2 = 6.07710050630396597660e-11;
    private static final double PIO2_2T = 2.02226624879595063154e-21;
    private static final double PIO2_3 = 2.02226624871116645580e-21;
    private static final double PIO2_3T = 8.47842766036889956997e-32;

    private static int remPio2(double x, double[] y) {
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        if (ix <= 0x3fe921fb) {
            y[0] = x;
            y[1] = 0;
            return 0;
        }
        if (ix < 0x4002d97c) {
            double z;
            if (hx > 0) {
                z = x - PIO2_1;
                if (ix != 0x3ff921fb) {
                    y[0] = z - PIO2_1T;
                    y[1] = (z - y[0]) - PIO2_1T;
                } else {
                    z -= PIO2_2;
                    y[0] = z - PIO2_2T;
                    y[1] = (z - y[0]) - PIO2_2T;
                }
                return 1;
            }
            z = x + PIO2_1;
            if (ix != 0x3ff921fb) {
                y[0] = z + PIO2_1T;
                y[1] = (z - y[0]) + PIO2_1T;
            } else {
                z += PIO2_2;
                y[0] = z + PIO2_2T;
                y[1] = (z - y[0]) + PIO2_2T;
            }
            return -1;
        }
        if (ix <= 0x413921fb) {
            double t = abs(x);
            int n = (int) (t * INVPIO2 + HALF);
            double fn = n;
            double r = t - fn * PIO2_1;
            double w = fn * PIO2_1T;
            if (n < 32 && ix != NPIO2_HW[n - 1]) {
                y[0] = r - w;
            } else {
                int j = ix >> 20;
                y[0] = r - w;
                int i = j - ((hi(y[0]) >> 20) & 0x7ff);
                if (i > 16) {
                    t = r;
                    w = fn * PIO2_2;
                    r = t - w;
                    w = fn * PIO2_2T - ((t - r) - w);
                    y[0] = r - w;
                    i = j - ((hi(y[0]) >> 20) & 0x7ff);
                    if (i > 49) {
                        t = r;
                        w = fn * PIO2_3;
                        r = t - w;
                        w = fn * PIO2_3T - ((t - r) - w);
                        y[0] = r - w;
                    }
                }
            }
            y[1] = (r - y[0]) - w;
            if (hx < 0) {
                y[0] = -y[0];
                y[1] = -y[1];
                return -n;
            }
            return n;
        }
        if (ix >= 0x7ff00000) {
            y[0] = x - x;
            y[1] = y[0];
            return 0;
        }
        int e0 = (ix >> 20) - 1046;
        double z = words(ix - (e0 << 20), lo(x));
        double[] tx = new double[3];
        for (int i = 0; i < 2; i++) {
            tx[i] = (int) z;
            z = (z - tx[i]) * TWO24;
        }
        tx[2] = z;
        int nx = 3;
        while (tx[nx - 1] == ZERO) {
            nx--;
        }
        int n = kernelRemPio2(tx, y, e0, nx);
        if (hx < 0) {
            y[0] = -y[0];
            y[1] = -y[1];
            return -n;
        }
        return n;
    }

    private static final double[] PIO2 = {
        1.57079625129699707031e+00,
        7.54978941586159635335e-08,
        5.39030252995776476554e-15,
        3.28200341580791294123e-22,
        1.27065575308067607349e-29,
        1.22933308981111328932e-36,
        2.73370053816464559624e-44,
        2.16741683877804819444e-51,
    };

    /** reduces a large argument with prec 2, to two doubles */
    private static int kernelRemPio2(double[] x, double[] y, int e0, int nx) {
        int jk = 4;
        int jp = jk;
        int jx = nx - 1;
        int jv = Math.max((e0 - 3) / 24, 0);
        int q0 = e0 - 24 * (jv + 1);
        double[] f = new double[20];
        double[] q = new double[20];
        double[] fq = new double[20];
        int[] iq = new int[20];
        int j = jv - jx;
        int m = jx + jk;
        for (int i = 0; i <= m; i++, j++) {
            f[i] = j < 0 ? ZERO : (double) TWO_OVER_PI[j];
        }
        for (int i = 0; i <= jk; i++) {
            double fw = 0.0;
            for (j = 0; j <= jx; j++) {
                fw += x[j] * f[jx + i - j];
            }
            q[i] = fw;
        }
        int jz = jk;
        int n;
        int ih;
        double z;
        while (true) {
            int i = 0;
            j = jz;
            z = q[jz];
            while (j > 0) {
                double fw = (int) (TWON24 * z);
                iq[i] = (int) (z - TWO24 * fw);
                z = q[j - 1] + fw;
                i++;
                j--;
            }
            z = scalbn(z, q0);
            z -= 8.0 * floor(z * 0.125);
            n = (int) z;
            z -= n;
            ih = 0;
            if (q0 > 0) {
                i = iq[jz - 1] >> (24 - q0);
                n += i;
                iq[jz - 1] -= i << (24 - q0);
                ih = iq[jz - 1] >> (23 - q0);
            } else if (q0 == 0) {
                ih = iq[jz - 1] >> 23;
            } else if (z >= 0.5) {
                ih = 2;
            }
            if (ih > 0) {
                n += 1;
                int carry = 0;
                for (i = 0; i < jz; i++) {
                    j = iq[i];
                    if (carry == 0) {
                        if (j != 0) {
                            carry = 1;
                            iq[i] = 0x1000000 - j;
                        }
                    } else {
                        iq[i] = 0xffffff - j;
                    }
                }
                if (q0 == 1) {
                    iq[jz - 1] &= 0x7fffff;
                } else if (q0 == 2) {
                    iq[jz - 1] &= 0x3fffff;
                }
                if (ih == 2) {
                    z = ONE - z;
                    if (carry != 0) {
                        z -= scalbn(ONE, q0);
                    }
                }
            }
            if (z == ZERO) {
                j = 0;
                for (i = jz - 1; i >= jk; i--) {
                    j |= iq[i];
                }
                if (j == 0) {
                    int k = 1;
                    while (iq[jk - k] == 0) {
                        k++;
                    }
                    for (i = jz + 1; i <= jz + k; i++) {
                        f[jx + i] = TWO_OVER_PI[jv + i];
                        double fw = 0.0;
                        for (j = 0; j <= jx; j++) {
                            fw += x[j] * f[jx + i - j];
                        }
                        q[i] = fw;
                    }
                    jz += k;
                    continue;
                }
            }
            break;
        }
        if (z == 0.0) {
            jz -= 1;
            q0 -= 24;
            while (iq[jz] == 0) {
                jz--;
                q0 -= 24;
            }
        } else {
            z = scalbn(z, -q0);
            if (z >= TWO24) {
                double fw = (int) (TWON24 * z);
                iq[jz] = (int) (z - TWO24 * fw);
                jz += 1;
                q0 += 24;
                iq[jz] = (int) fw;
            } else {
                iq[jz] = (int) z;
            }
        }
        double fw = scalbn(ONE, q0);
        for (int i = jz; i >= 0; i--) {
            q[i] = fw * iq[i];
            fw *= TWON24;
        }
        for (int i = jz; i >= 0; i--) {
            fw = 0.0;
            for (int k = 0; k <= jp && k <= jz - i; k++) {
                fw += PIO2[k] * q[i + k];
            }
            fq[jz - i] = fw;
        }
        fw = 0.0;
        for (int i = jz; i >= 0; i--) {
            fw += fq[i];
        }
        y[0] = ih == 0 ? fw : -fw;
        fw = fq[0] - fw;
        for (int i = 1; i <= jz; i++) {
            fw += fq[i];
        }
        y[1] = ih == 0 ? fw : -fw;
        return n & 7;
    }

    public static double sin(double x) {
        int ix = hi(x) & 0x7fffffff;
        if (ix <= 0x3fe921fb) {
            return kernelSin(x, 0.0, 0);
        } else if (ix >= 0x7ff00000) {
            return x - x;
        }
        double[] y = new double[2];
        int n = remPio2(x, y);
        switch (n & 3) {
            case 0:
                return kernelSin(y[0], y[1], 1);
            case 1:
                return kernelCos(y[0], y[1]);
            case 2:
                return -kernelSin(y[0], y[1], 1);
            default:
                return -kernelCos(y[0], y[1]);
        }
    }

    public static double cos(double x) {
        int ix = hi(x) & 0x7fffffff;
        if (ix <= 0x3fe921fb) {
            return kernelCos(x, 0.0);
        } else if (ix >= 0x7ff00000) {
            return x - x;
        }
        double[] y = new double[2];
        int n = remPio2(x, y);
        switch (n & 3) {
            case 0:
                return kernelCos(y[0], y[1]);
            case 1:
                return -kernelSin(y[0], y[1], 1);
            case 2:
                return -kernelCos(y[0], y[1]);
            default:
                return kernelSin(y[0], y[1], 1);
        }
    }

    public static double tan(double x) {
        int ix = hi(x) & 0x7fffffff;
        if (ix <= 0x3fe921fb) {
            return kernelTan(x, 0.0, 1);
        } else if (ix >= 0x7ff00000) {
            return x - x;
        }
        double[] y = new double[2];
        int n = remPio2(x, y);
        return kernelTan(y[0], y[1], 1 - ((n & 1) << 1));
    }

    // inverse trigonometry

    private static final double[] ATANHI = {
        4.63647609000806093515e-01,
        7.85398163397448278999e-01,
        9.82793723247329054082e-01,
        1.57079632679489655800e+00,
    };
    private static final double[] ATANLO = {
        2.26987774529616870924e-17,
        3.06161699786838301793e-17,
        1.39033110312309984516e-17,
        6.12323399573676603587e-17,
    };
    private static final double[] AT = {
        3.33333333333329318027e-01,
        -1.99999999998764832476e-01,
        1.42857142725034663711e-01,
        -1.11111104054623557880e-01,
        9.09088713343650656196e-02,
        -7.69187620504482999495e-02,
        6.66107313738753120669e-02,
        -5.83357013379057348645e-02,
        4.97687799461593236017e-02,
        -3.65315727442169155270e-02,
        1.62858201153657823623e-02,
    };

    public static double atan(double x) {
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        int id;
        if (ix >= 0x44100000) {
            if (ix > 0x7ff00000 || (ix == 0x7ff00000 && lo(x) != 0)) {
                return x + x;
            }
            return hx > 0 ? ATANHI[3] + ATANLO[3] : -ATANHI[3] - ATANLO[3];
        }
        if (ix < 0x3fdc0000) {
            if (ix < 0x3e200000) {
                if (HUGE + x > ONE) {
                    return x;
                }
            }
            id = -1;
        } else {
            x = abs(x);
            if (ix < 0x3ff30000) {
                if (ix < 0x3fe60000) {
                    id = 0;
                    x = (2.0 * x - ONE) / (2.0 + x);
                } else {
                    id = 1;
                    x = (x - ONE) / (x + ONE);
                }
            } else {
                if (ix < 0x40038000) {
                    id = 2;
                    x = (x - 1.5) / (ONE + 1.5 * x);
                } else {
                    id = 3;
                    x = -1.0 / x;
                }
            }
        }
        double z = x * x;
        double w = z * z;
        double s1 = z * (AT[0] + w * (AT[2] + w * (AT[4] + w * (AT[6] + w * (AT[8] + w * AT[10])))));
        double s2 = w * (AT[1] + w * (AT[3] + w * (AT[5] + w * (AT[7] + w * AT[9]))));
        if (id < 0) {
            return x - x * (s1 + s2);
        }
        z = ATANHI[id] - ((x * (s1 + s2) - ATANLO[id]) - x);
        return hx < 0 ? -z : z;
    }

    private static final double PI_O_4 = 7.8539816339744827900E-01;
    private static final double PI_O_2 = 1.5707963267948965580E+00;
    private static final double PI = 3.1415926535897931160E+00;
    private static final double PI_LO = 1.2246467991473531772E-16;

    public static double atan2(double y, double x) {
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        int lx = lo(x);
        int hy = hi(y);
        int iy = hy & 0x7fffffff;
        int ly = lo(y);
        if ((ix | ((lx | -lx) >>> 31)) > 0x7ff00000 || (iy | ((ly | -ly) >>> 31)) > 0x7ff00000) {
            return x + y;
        }
        if (((hx - 0x3ff00000) | lx) == 0) {
            return atan(y);
        }
        int m = ((hy >> 31) & 1) | ((hx >> 30) & 2);
        if ((iy | ly) == 0) {
            switch (m) {
                case 0:
                case 1:
                    return y;
                case 2:
                    return PI + TINY;
                default:
                    return -PI - TINY;
            }
        }
        if ((ix | lx) == 0) {
            return hy < 0 ? -PI_O_2 - TINY : PI_O_2 + TINY;
        }
        if (ix == 0x7ff00000) {
            if (iy == 0x7ff00000) {
                switch (m) {
                    case 0:
                        return PI_O_4 + TINY;
                    case 1:
                        return -PI_O_4 - TINY;
                    case 2:
                        return 3.0 * PI_O_4 + TINY;
                    default:
                        return -3.0 * PI_O_4 - TINY;
                }
            }
            switch (m) {
                case 0:
                    return ZERO;
                case 1:
                    return -ZERO;
                case 2:
                    return PI + TINY;
                default:
                    return -PI - TINY;
            }
        }
        if (iy == 0x7ff00000) {
            return hy < 0 ? -PI_O_2 - TINY : PI_O_2 + TINY;
        }
        int k = (iy - ix) >> 20;
        double z;
        if (k > 60) {
            z = PI_O_2 + 0.5 * PI_LO;
        } else if (hx < 0 && k < -60) {
            z = 0.0;
        } else {
            z = atan(abs(y / x));
        }
        switch (m) {
            case 0:
                return z;
            case 1:
                return -z;
            case 2:
                return PI - (z - PI_LO);
            default:
                return (z - PI_LO) - PI;
        }
    }

    private static final double PIO2_HI = 1.57079632679489655800e+00;
    private static final double PIO2_LO = 6.12323399573676603587e-17;
    private static final double PIO4_HI = 7.85398163397448278999e-01;
    private static final double PS0 = 1.66666666666666657415e-01;
    private static final double PS1 = -3.25565818622400915405e-01;
    private static final double PS2 = 2.01212532134862925881e-01;
    private static final double PS3 = -4.00555345006794114027e-02;
    private static final double PS4 = 7.91534994289814532176e-04;
    private static final double PS5 = 3.47933107596021167570e-05;
    private static final double QS1 = -2.40339491173441421878e+00;
    private static final double QS2 = 2.02094576023350569471e+00;
    private static final double QS3 = -6.88283971605453293030e-01;
    private static final double QS4 = 7.70381505559019352791e-02;

    private static double asinP(double t) {
        return t * (PS0 + t * (PS1 + t * (PS2 + t * (PS3 + t * (PS4 + t * PS5)))));
    }

    private static double asinQ(double t) {
        return ONE + t * (QS1 + t * (QS2 + t * (QS3 + t * QS4)));
    }

    public static double asin(double x) {
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        if (ix >= 0x3ff00000) {
            if (((ix - 0x3ff00000) | lo(x)) == 0) {
                return x * PIO2_HI + x * PIO2_LO;
            }
            return (x - x) / (x - x);
        } else if (ix < 0x3fe00000) {
            if (ix < 0x3e400000) {
                if (HUGE + x > ONE) {
                    return x;
                }
            } else {
                double t = x * x;
                double w = asinP(t) / asinQ(t);
                return x + x * w;
            }
        }
        double w = ONE - abs(x);
        double t = w * 0.5;
        double p = asinP(t);
        double q = asinQ(t);
        double s = sqrt(t);
        if (ix >= 0x3FEF3333) {
            w = p / q;
            t = PIO2_HI - (2.0 * (s + s * w) - PIO2_LO);
        } else {
            w = withLow(s, 0);
            double c = (t - w * w) / (s + w);
            double r = p / q;
            p = 2.0 * s * r - (PIO2_LO - 2.0 * c);
            q = PIO4_HI - 2.0 * w;
            t = PIO4_HI - (p - q);
        }
        return hx > 0 ? t : -t;
    }

    public static double acos(double x) {
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        if (ix >= 0x3ff00000) {
            if (((ix - 0x3ff00000) | lo(x)) == 0) {
                return hx > 0 ? 0.0 : PI + 2.0 * PIO2_LO;
            }
            return (x - x) / (x - x);
        }
        if (ix < 0x3fe00000) {
            if (ix <= 0x3c600000) {
                return PIO2_HI + PIO2_LO;
            }
            double z = x * x;
            double r = asinP(z) / asinQ(z);
            return PIO2_HI - (x - (PIO2_LO - x * r));
        } else if (hx < 0) {
            double z = (ONE + x) * 0.5;
            double s = sqrt(z);
            double r = asinP(z) / asinQ(z);
            double w = r * s - PIO2_LO;
            return PI - 2.0 * (s + w);
        }
        double z = (ONE - x) * 0.5;
        double s = sqrt(z);
        double df = withLow(s, 0);
        double c = (z - df * df) / (s + df);
        double r = asinP(z) / asinQ(z);
        double w = r * s + c;
        return 2.0 * (df + w);
    }

    // exponent and logarithm

    private static final double[] HALF_SIGNED = {0.5, -0.5};
    private static final double TWOM1000 = 9.33263618503218878990e-302;
    private static final double O_THRESHOLD = 7.09782712893383973096e+02;
    private static final double U_THRESHOLD = -7.45133219101941108420e+02;
    private static final double[] LN2HI = {6.93147180369123816490e-01, -6.93147180369123816490e-01};
    private static final double[] LN2LO = {1.90821492927058770002e-10, -1.90821492927058770002e-10};
    private static final double INVLN2 = 1.44269504088896338700e+00;
    private static final double P1 = 1.66666666666666019037e-01;
    private static final double P2 = -2.77777777770155933842e-03;
    private static final double P3 = 6.61375632143793436117e-05;
    private static final double P4 = -1.65339022054652515390e-06;
    private static final double P5 = 4.13813679705723846039e-08;

    public static double exp(double x) {
        int hx = hi(x);
        int xsb = (hx >> 31) & 1;
        hx &= 0x7fffffff;
        double high = 0;
        double low = 0;
        int k = 0;
        if (hx >= 0x40862E42) {
            if (hx >= 0x7ff00000) {
                if (((hx & 0xfffff) | lo(x)) != 0) {
                    return x + x;
                }
                return xsb == 0 ? x : 0.0;
            }
            if (x > O_THRESHOLD) {
                return HUGE * HUGE;
            }
            if (x < U_THRESHOLD) {
                return TWOM1000 * TWOM1000;
            }
        }
        if (hx > 0x3fd62e42) {
            if (hx < 0x3FF0A2B2) {
                high = x - LN2HI[xsb];
                low = LN2LO[xsb];
                k = 1 - xsb - xsb;
            } else {
                k = (int) (INVLN2 * x + HALF_SIGNED[xsb]);
                double t = k;
                high = x - t * LN2HI[0];
                low = t * LN2LO[0];
            }
            x = high - low;
        } else if (hx < 0x3e300000) {
            if (HUGE + x > ONE) {
                return ONE + x;
            }
        }
        double t = x * x;
        double c = x - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
        if (k == 0) {
            return ONE - ((x * c) / (c - 2.0) - x);
        }
        double y = ONE - ((low - (x * c) / (2.0 - c)) - high);
        if (k >= -1021) {
            return withHigh(y, hi(y) + (k << 20));
        }
        y = withHigh(y, hi(y) + ((k + 1000) << 20));
        return y * TWOM1000;
    }

    private static final double LN2_HI = 6.93147180369123816490e-01;
    private static final double LN2_LO = 1.90821492927058770002e-10;
    private static final double LG1 = 6.666666666666735130e-01;
    private static final double LG2 = 3.999999999940941908e-01;
    private static final double LG3 = 2.857142874366239149e-01;
    private static final double LG4 = 2.222219843214978396e-01;
    private static final double LG5 = 1.818357216161805012e-01;
    private static final double LG6 = 1.531383769920937332e-01;
    private static final double LG7 = 1.479819860511658591e-01;

    public static double log(double x) {
        int hx = hi(x);
        int lx = lo(x);
        int k = 0;
        if (hx < 0x00100000) {
            if (((hx & 0x7fffffff) | lx) == 0) {
                return -TWO54 / ZERO;
            }
            if (hx < 0) {
                return (x - x) / ZERO;
            }
            k -= 54;
            x *= TWO54;
            hx = hi(x);
        }
        if (hx >= 0x7ff00000) {
            return x + x;
        }
        k += (hx >> 20) - 1023;
        hx &= 0x000fffff;
        int i = (hx + 0x95f64) & 0x100000;
        x = withHigh(x, hx | (i ^ 0x3ff00000));
        k += i >> 20;
        double f = x - 1.0;
        double dk;
        if ((0x000fffff & (2 + hx)) < 3) {
            if (f == ZERO) {
                if (k == 0) {
                    return ZERO;
                }
                dk = k;
                return dk * LN2_HI + dk * LN2_LO;
            }
            double r = f * f * (0.5 - 0.33333333333333333 * f);
            if (k == 0) {
                return f - r;
            }
            dk = k;
            return dk * LN2_HI - ((r - dk * LN2_LO) - f);
        }
        double s = f / (2.0 + f);
        dk = k;
        double z = s * s;
        i = hx - 0x6147a;
        double w = z * z;
        int j = 0x6b851 - hx;
        double t1 = w * (LG2 + w * (LG4 + w * LG6));
        double t2 = z * (LG1 + w * (LG3 + w * (LG5 + w * LG7)));
        i |= j;
        double r = t2 + t1;
        if (i > 0) {
            double hfsq = 0.5 * f * f;
            if (k == 0) {
                return f - (hfsq - s * (hfsq + r));
            }
            return dk * LN2_HI - ((hfsq - (s * (hfsq + r) + dk * LN2_LO)) - f);
        }
        if (k == 0) {
            return f - s * (f - r);
        }
        return dk * LN2_HI - ((s * (f - r) - dk * LN2_LO) - f);
    }

    private static final double[] BP = {1.0, 1.5};
    private static final double[] DP_H = {0.0, 5.84962487220764160156e-01};
    private static final double[] DP_L = {0.0, 1.35003920212974897128e-08};
    private static final double TWO53 = 9007199254740992.0;
    private static final double L1 = 5.99999999999994648725e-01;
    private static final double L2 = 4.28571428578550184252e-01;
    private static final double L3 = 3.33333329818377432918e-01;
    private static final double L4 = 2.72728123808534006489e-01;
    private static final double L5 = 2.30660745775561754067e-01;
    private static final double L6 = 2.06975017800338417784e-01;
    private static final double LG2_ = 6.93147180559945286227e-01;
    private static final double LG2_H = 6.93147182464599609375e-01;
    private static final double LG2_L = -1.90465429995776804525e-09;
    private static final double OVT = 8.0085662595372944372e-0017;
    private static final double CP = 9.61796693925975554329e-01;
    private static final double CP_H = 9.61796700954437255859e-01;
    private static final double CP_L = -7.02846165095275826516e-09;
    private static final double IVLN2 = 1.44269504088896338700e+00;
    private static final double IVLN2_H = 1.44269502162933349609e+00;
    private static final double IVLN2_L = 1.92596299112661746887e-08;

    public static double pow(double x, double y) {
        int hx = hi(x);
        int lx = lo(x);
        int hy = hi(y);
        int ly = lo(y);
        int ix = hx & 0x7fffffff;
        int iy = hy & 0x7fffffff;
        if ((iy | ly) == 0) {
            return ONE;
        }
        if (ix > 0x7ff00000 || (ix == 0x7ff00000 && lx != 0) || iy > 0x7ff00000
                || (iy == 0x7ff00000 && ly != 0)) {
            return x + y;
        }
        int yisint = 0;
        if (hx < 0) {
            if (iy >= 0x43400000) {
                yisint = 2;
            } else if (iy >= 0x3ff00000) {
                int k = (iy >> 20) - 0x3ff;
                if (k > 20) {
                    int j = ly >>> (52 - k);
                    if ((j << (52 - k)) == ly) {
                        yisint = 2 - (j & 1);
                    }
                } else if (ly == 0) {
                    int j = iy >> (20 - k);
                    if ((j << (20 - k)) == iy) {
                        yisint = 2 - (j & 1);
                    }
                }
            }
        }
        if (ly == 0) {
            if (iy == 0x7ff00000) {
                if (((ix - 0x3ff00000) | lx) == 0) {
                    return y - y;
                } else if (ix >= 0x3ff00000) {
                    return hy >= 0 ? y : ZERO;
                }
                return hy < 0 ? -y : ZERO;
            }
            if (iy == 0x3ff00000) {
                return hy < 0 ? ONE / x : x;
            }
            if (hy == 0x40000000) {
                return x * x;
            }
            if (hy == 0x3fe00000) {
                if (hx >= 0) {
                    return sqrt(x);
                }
            }
        }
        double ax = abs(x);
        if (lx == 0) {
            if (ix == 0x7ff00000 || ix == 0 || ix == 0x3ff00000) {
                double z = ax;
                if (hy < 0) {
                    z = ONE / z;
                }
                if (hx < 0) {
                    if (((ix - 0x3ff00000) | yisint) == 0) {
                        z = (z - z) / (z - z);
                    } else if (yisint == 1) {
                        z = -z;
                    }
                }
                return z;
            }
        }
        int n = (hx >>> 31) - 1;
        if ((n | yisint) == 0) {
            return (x - x) / (x - x);
        }
        double s = ONE;
        if ((n | (yisint - 1)) == 0) {
            s = -ONE;
        }
        double t1;
        double t2;
        if (iy > 0x41e00000) {
            if (iy > 0x43f00000) {
                if (ix <= 0x3fefffff) {
                    return hy < 0 ? HUGE * HUGE : TINY * TINY;
                }
                if (ix >= 0x3ff00000) {
                    return hy > 0 ? HUGE * HUGE : TINY * TINY;
                }
            }
            if (ix < 0x3fefffff) {
                return hy < 0 ? s * HUGE * HUGE : s * TINY * TINY;
            }
            if (ix > 0x3ff00000) {
                return hy > 0 ? s * HUGE * HUGE : s * TINY * TINY;
            }
            double t = ax - ONE;
            double w = (t * t) * (0.5 - t * (0.3333333333333333333333 - t * 0.25));
            double u = IVLN2_H * t;
            double v = t * IVLN2_L - w * IVLN2;
            t1 = withLow(u + v, 0);
            t2 = v - (t1 - u);
        } else {
            n = 0;
            if (ix < 0x00100000) {
                ax *= TWO53;
                n -= 53;
                ix = hi(ax);
            }
            n += (ix >> 20) - 0x3ff;
            int j = ix & 0x000fffff;
            ix = j | 0x3ff00000;
            int k;
            if (j <= 0x3988E) {
                k = 0;
            } else if (j < 0xBB67A) {
                k = 1;
            } else {
                k = 0;
                n += 1;
                ix -= 0x00100000;
            }
            ax = withHigh(ax, ix);
            double u = ax - BP[k];
            double v = ONE / (ax + BP[k]);
            double ss = u * v;
            double sh = withLow(ss, 0);
            double th = words(((ix >> 1) | 0x20000000) + 0x00080000 + (k << 18), 0);
            double tl = ax - (th - BP[k]);
            double sl = v * ((u - sh * th) - sh * tl);
            double s2 = ss * ss;
            double r = s2 * s2 * (L1 + s2 * (L2 + s2 * (L3 + s2 * (L4 + s2 * (L5 + s2 * L6)))));
            r += sl * (sh + ss);
            s2 = sh * sh;
            th = withLow(3.0 + s2 + r, 0);
            tl = r - ((th - 3.0) - s2);
            u = sh * th;
            v = sl * th + tl * ss;
            double ph = withLow(u + v, 0);
            double pl = v - (ph - u);
            double zh = CP_H * ph;
            double zl = CP_L * ph + pl * CP + DP_L[k];
            double t = n;
            t1 = withLow(((zh + zl) + DP_H[k]) + t, 0);
            t2 = zl - (((t1 - t) - DP_H[k]) - zh);
        }
        double y1 = withLow(y, 0);
        double pl = (y - y1) * t1 + y * t2;
        double ph = y1 * t1;
        double z = pl + ph;
        int j = hi(z);
        int i = lo(z);
        if (j >= 0x40900000) {
            if (((j - 0x40900000) | i) != 0) {
                return s * HUGE * HUGE;
            } else if (pl + OVT > z - ph) {
                return s * HUGE * HUGE;
            }
        } else if ((j & 0x7fffffff) >= 0x4090cc00) {
            if (((j - 0xc090cc00) | i) != 0) {
                return s * TINY * TINY;
            } else if (pl <= z - ph) {
                return s * TINY * TINY;
            }
        }
        i = j & 0x7fffffff;
        int k = (i >> 20) - 0x3ff;
        n = 0;
        if (i > 0x3fe00000) {
            n = j + (0x00100000 >> (k + 1));
            k = ((n & 0x7fffffff) >> 20) - 0x3ff;
            double t = words(n & ~(0x000fffff >> k), 0);
            n = ((n & 0x000fffff) | 0x00100000) >> (20 - k);
            if (j < 0) {
                n = -n;
            }
            ph -= t;
        }
        double t = withLow(pl + ph, 0);
        double u = t * LG2_H;
        double v = (pl - (t - ph)) * LG2_ + t * LG2_L;
        z = u + v;
        double w = v - (z - u);
        t = z * z;
        t1 = z - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
        double r = (z * t1) / (t1 - 2.0) - (w + z * w);
        z = ONE - (r - z);
        j = hi(z);
        j += n << 20;
        if ((j >> 20) <= 0) {
            z = scalbn(z, n);
        } else {
            z = withHigh(z, hi(z) + (n << 20));
        }
        return s * z;
    }
}
