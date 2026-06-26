package com.iseeethan.mantle.world.gen.tectonics;

import java.util.Arrays;

final class Drainage {

    private static final int[] DX = {  0,  0, -1,  1, -1,  1, -1,  1 };
    private static final int[] DZ = { -1,  1,  0,  0, -1, -1,  1,  1 };
    private static final double[] DIST = { 1, 1, 1, 1, Math.sqrt(2), Math.sqrt(2), Math.sqrt(2), Math.sqrt(2) };

    private final int n;
    private final float seaY;

    final float[] filled;

    final int[] downstream;

    final int[] accum;

    final double[] distDown;

    final int[] popOrder;

    private final float[] heapKey;
    private final int[] heapVal;
    private int heapSize;
    private final boolean[] closed;

    Drainage(int n, float seaY) {
        this.n = n;
        this.seaY = seaY;
        int sz = n * n;
        filled = new float[sz];
        downstream = new int[sz];
        accum = new int[sz];
        distDown = new double[sz];
        popOrder = new int[sz];
        heapKey = new float[sz + 1];
        heapVal = new int[sz + 1];
        closed = new boolean[sz];
    }

    void solve(float[] elev) {
        priorityFlood(elev);
        flowAccumulation();
    }

    private void priorityFlood(float[] elev) {
        Arrays.fill(closed, false);
        heapSize = 0;
        int orderSeq = 0;

        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                boolean edge = (i == 0 || j == 0 || i == n - 1 || j == n - 1);
                boolean ocean = elev[idx] <= seaY;
                if (edge || ocean) {
                    closed[idx] = true;
                    filled[idx] = Math.max(elev[idx], seaY);
                    downstream[idx] = -1;
                    distDown[idx] = 0;
                    push(filled[idx], idx);
                }
            }
        }

        int popped = 0;
        while (heapSize > 0) {
            int cur = popMinVal();
            popOrder[popped++] = cur;
            int ci = cur % n, cj = cur / n;
            for (int d = 0; d < 8; d++) {
                int ni = ci + DX[d], nj = cj + DZ[d];
                if (ni < 0 || nj < 0 || ni >= n || nj >= n) continue;
                int nidx = nj * n + ni;
                if (closed[nidx]) continue;
                closed[nidx] = true;
                filled[nidx] = Math.max(elev[nidx], filled[cur]);
                downstream[nidx] = cur;
                distDown[nidx] = DIST[d];
                push(filled[nidx], nidx);
            }
        }
    }

    private void flowAccumulation() {
        Arrays.fill(accum, 1);
        for (int k = popOrder.length - 1; k >= 0; k--) {
            int idx = popOrder[k];
            int ds = downstream[idx];
            if (ds >= 0) accum[ds] += accum[idx];
        }
    }

    private void push(float key, int val) {
        int i = ++heapSize;
        heapKey[i] = key; heapVal[i] = val;
        while (i > 1) {
            int p = i >> 1;
            if (heapKey[p] <= heapKey[i]) break;
            swap(p, i); i = p;
        }
    }

    private int popMinVal() {
        int top = heapVal[1];
        heapKey[1] = heapKey[heapSize]; heapVal[1] = heapVal[heapSize]; heapSize--;
        int i = 1;
        while (true) {
            int l = i << 1, r = l + 1, m = i;
            if (l <= heapSize && heapKey[l] < heapKey[m]) m = l;
            if (r <= heapSize && heapKey[r] < heapKey[m]) m = r;
            if (m == i) break;
            swap(m, i); i = m;
        }
        return top;
    }

    private void swap(int a, int b) {
        float k = heapKey[a]; heapKey[a] = heapKey[b]; heapKey[b] = k;
        int v = heapVal[a]; heapVal[a] = heapVal[b]; heapVal[b] = v;
    }
}
