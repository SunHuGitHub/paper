package com.tiger.paper;

import java.io.FileNotFoundException;
//https://blog.csdn.net/cc098818/article/details/99869166
public class Tabu {
    //初始化编码Ghh
    public static void initGroup() {
        Data.Ghh[0] = Data.random.nextInt(65535) % Data.cityNum;
        int i, j;
        //使得产生的每个基因都不一样
        for (i = 1; i < Data.cityNum; ) {
            Data.Ghh[i] = Data.random.nextInt(65535) % Data.cityNum;
            for (j = 0; j < i; j++) {
                if (Data.Ghh[i] == Data.Ghh[j]) {
                    break;
                }
            }
            if (j == i) {
                i++;
            }
        }
    }

    //复制操作，将Gha复制到Ghb
    public static void copyGh(int[] Gha, int[] Ghb) {
        for (int i = 0; i < Data.cityNum; i++) {
            Ghb[i] = Gha[i];
        }
    }

    //评价函数
    public static int evaluate(int[] chr) {
        int len = 0;
        for (int i = 1; i < Data.cityNum; i++) {
            len += Data.dist[chr[i - 1]][chr[i]];
        }
        len += Data.dist[chr[Data.cityNum - 1]][chr[0]];
        return len;
    }

    //领域交换，得到邻居
    public static void Linju(int[] Gh, int[] tempGh) {
        int i, temp;
        int rand1, rand2;
        //将Gh复制到tempGh
        for (i = 0; i < Data.cityNum; i++) {
            tempGh[i] = Gh[i];
        }
        rand1 = Data.random.nextInt(65535) % Data.cityNum;
        rand2 = Data.random.nextInt(65535) % Data.cityNum;
        while (rand1 == rand2) {
            rand2 = Data.random.nextInt(65535) % Data.cityNum;
        }
        //交换
        temp = tempGh[rand1];
        tempGh[rand1] = tempGh[rand2];
        tempGh[rand2] = temp;
    }

    //判断编码是否在禁忌表中
    public static int panduan(int[] tempGh) {
        int i, j;
        int flag = 0;
        for (i = 0; i < Data.ll; i++) {
            flag = 0;
            for (j = 0; j < Data.cityNum; j++) {
                if (Data.tempGh[j] != Data.jinji[i][j]) {
                    flag = 1;//不相同
                    break;
                }
            }
            if (flag == 0) {    //相同，返回存在相同
                break;
            }
        }
        if (i == Data.ll) {
            return 0;//不存在
        } else {
            return 1;//存在
        }
    }

    //解禁忌与加入禁忌
    public static void jiejinji(int[] tempGh) {
        int i, j, k;
        //删除禁忌表第一个编码，后面编码往前移动
        for (i = 0; i < Data.ll - 1; i++) {
            for (j = 0; j < Data.cityNum; j++) {
                Data.jinji[i][j] = Data.jinji[i + 1][j];
            }
        }
        //新的编码加入禁忌表
        for (k = 0; k < Data.cityNum; k++) {
            Data.jinji[Data.ll - 1][k] = tempGh[k];
        }
    }

    public static void solve() {
        int nn;
        //初始化编码Ghh
        initGroup();
        copyGh(Data.Ghh, Data.bestGh);//复制当前编码Ghh到最好编码bestGh
        Data.bestEvaluation = evaluate(Data.Ghh);

        while (Data.t < Data.MAX_GEN) {
            nn = 0;
            Data.LocalEvaluation = Integer.MAX_VALUE;
            while (nn < Data.N) {
                Linju(Data.Ghh, Data.tempGh);//得到当前编码Ghh到邻居编码tempGh
                if (panduan(Data.tempGh) == 0) {//判断是否在禁忌表中
                    //不在
                    Data.tempEvaluation = evaluate(Data.tempGh);
                    if (Data.tempEvaluation < Data.LocalEvaluation) {
                        copyGh(Data.tempGh, Data.LocalGh);
                        Data.LocalEvaluation = Data.tempEvaluation;
                    }
                    nn++;
                }
            }
            if (Data.LocalEvaluation < Data.bestEvaluation) {
                Data.bestT = Data.t;
                copyGh(Data.LocalGh, Data.bestGh);
                Data.bestEvaluation = Data.LocalEvaluation;
            }
            copyGh(Data.LocalGh, Data.Ghh);

            //解禁忌表，LocalGh加入禁忌表
            jiejinji(Data.LocalGh);
            Data.t++;
        }
        System.out.println("最佳迭代次数:" + Data.bestT);
        System.out.println("最佳长度为:" + Data.bestEvaluation);
        System.out.println("最佳路径为:");
        for (int i = 0; i < Data.cityNum; i++) {
            System.out.print(Data.bestGh[i] + "-->");
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        Data.read_data("data/att48.txt");
        Tabu.solve();
    }
}