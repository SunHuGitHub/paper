package com.tiger.paper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Random;
import java.util.Scanner;

public class Data {

    public static final int MAX_GEN = 1000;//最大的迭代次数
    public static final int N = 200;//每次搜索领域的个数
    public static final int ll = 20;//禁忌长度
    public static int cityNum = 10;//城市数量，手动设置
    public static int jinji[][] = new int[ll][cityNum];//禁忌表
    public static int[] Ghh = new int[cityNum];//初始路径编码
    public static int bestGh[] = new int[cityNum];//最好的路径编码
    public static int[] LocalGh = new int[cityNum];//当前最好路径编码
    public static int[] tempGh = new int[cityNum];//存放临时编码
    public static int bestT;//最佳的迭代次数
    public static int bestEvaluation;
    public static int LocalEvaluation;
    public static int tempEvaluation;
    public static int point[][] = new int[cityNum][2];//每个城市的坐标
    public static int dist[][] = new int[cityNum][cityNum];//距离矩阵
    public static int t;//当前迭代
    public static Random random;

    private static int[][] cityPosition = {{0, 0}, {12, 32}, {5, 25}, {8, 45}, {33, 17},
            {25, 7}, {15, 15}, {15, 25}, {25, 15}, {41, 12}};

    //读取数据并初始化
    public static void read_data(String filepath) throws FileNotFoundException {
//		String line=null;
//		String substr[]=null;
//		Scanner cin=new Scanner(new BufferedReader(new FileReader(filepath)));
//		for (int i = 0; i < cityNum; i++) {
//			line=cin.nextLine();
//			line.trim();
//			substr=line.split(" ");
//			 point[i][0]=Integer.parseInt(substr[1]);//x坐标
//			 point[i][1]=Integer.parseInt(substr[2]);//y坐标
//		}
//		cin.close();
//		//计算距离矩阵，注意这里的计算方式，才用的是伪欧式距离
//		for (int i = 0; i < cityNum; i++) {
//			dist[i][i]=0;//对角线元素为0
//			for (int j = i+1; j < cityNum; j++) {
//				double rij=Math.sqrt((Math.pow(point[i][0]-point[j][0], 2)+
//						             Math.pow(point[i][1]-point[j][1], 2))/10.0);
//				//rij四舍五入取整
//				int tij=(int) Math.round(rij);
//				if(tij<rij) {
//					dist[i][j]=tij+1;
//					dist[j][i]=dist[i][j];
//				}else {
//					dist[i][j]=tij;
//					dist[j][i]=dist[i][j];
//				}
//			}
//		}
//		dist[cityNum-1][cityNum-1]=0;
        //初始化城市距离
        for (int i = 0; i < cityPosition.length; i++) {
            for (int j = i; j < cityPosition.length; j++) {
                dist[i][j] = (int) Math.sqrt(Math.pow(cityPosition[i][0] - cityPosition[j][0], 2) + Math.pow(cityPosition[i][1] - cityPosition[j][1], 2));
                dist[j][i] = dist[i][j];
            }
        }
        t = 0;
        bestT = 0;
        bestEvaluation = Integer.MAX_VALUE;
        LocalEvaluation = Integer.MAX_VALUE;
        tempEvaluation = Integer.MAX_VALUE;
        random = new Random(System.currentTimeMillis());
    }
}