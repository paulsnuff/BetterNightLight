package io.github.paulsnuff.betternightlight.shizuku;

interface IShizukuWorker {

    void destroy() = 16777114;

    int exec(in String[] cmd) = 1;
}