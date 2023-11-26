package com.bitasync.navplanner.util;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

import java.io.*;
import java.util.*;

/**
 * @author Lambda
 * @date 2023/11/23
 */
public class GeoDistanceCalculator
{
    static List<String[]> list;

    static
    {
        String readerFilePath = "src/main/resources/csv/geolocation_data.csv";
        try(BufferedReader reader = new BufferedReader(new FileReader(readerFilePath)))
        {
            list = new ArrayList<>();
            String line = reader.readLine();  // skip header line
            while ((line = reader.readLine()) != null)
            {
                String[] strs = new String[3];
                strs[0] = line.substring(0, line.indexOf(","));
                for(int i = 2; i > 0; --i)
                {
                    line = line.substring(0, line.lastIndexOf(","));
                    strs[i] = line.substring(line.lastIndexOf(",") + 1);
                }
                list.add(strs);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            System.out.println("geolocation_data文件读取完成");
        }
    }

    public static boolean getDistance4Dijkstra()
    {
        String writerFilePath = "src/main/resources/csv/path_data4Dijkstra.csv";
        Set<Integer> external = new HashSet<>(Arrays.asList(7, 8, 9, 10, 13, 14, 15, 16, 21, 22, 23, 24, 25, 26, 29, 30, 31, 32, 37, 38, 40, 41, 43, 44, 45, 46, 47, 48, 52, 53, 64, 65, 66, 67, 68, 69));
        Set<Integer>[] internal = new Set[]{ new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)), new HashSet<>(Arrays.asList(9, 10, 11, 12, 13, 14)), new HashSet<>(Arrays.asList(15, 16, 17, 18, 19, 20, 21, 22, 23, 24)), new HashSet<>(Arrays.asList(25, 26, 27, 28, 29, 30)), new HashSet<>(Arrays.asList(31, 32, 33, 34, 35, 36)), new HashSet<>(Arrays.asList(37, 38, 39, 40, 41, 42)), new HashSet<>(Arrays.asList(43, 44, 45)), new HashSet<>(Arrays.asList(46, 47, 48, 49, 50, 51)), new HashSet<>(Arrays.asList(52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63))};
        try
        {
            Set<String> info = new TreeSet<>();
            for(var source: external)
                for(var destination: external)
                    info.add(getDistance(source - 1, destination - 1));
            for(var set: internal)
                for(var source: set)
                    for(var destination: set)
                        info.add(getDistance(source - 1, destination - 1));
            BufferedWriter writer = new BufferedWriter(new FileWriter(writerFilePath, true));
            for (var str: info)
            {
                writer.newLine();
                writer.write(str);
            }
            System.out.println("文件写入完成");
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean getDistance4ACS()
    {
        String writerFilePath = "src/main/resources/csv/path_data4ACS.csv";
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(writerFilePath, true));
            for(int i = 0; i < list.size(); ++i)
                for(int j = 0; j < list.size(); ++j)
                {
                    writer.newLine();
                    writer.write(getDistance(i, j));
                }
            System.out.println("文件写入完成");
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean getDistance4Hybrid(String path, int[] points)
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write("start,end,weight");
            for(int i = 0; i < points.length; ++i)
                for(int j = 0; j < points.length; ++j)
                {
                    writer.newLine();
                    writer.write(getDistance(points[i] - 1, points[j] - 1));
                }
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static String getDistance(int source, int destination)
    {
        String[] strs1 = list.get(source);
        String[] strs2 = list.get(destination);

        double longitude1 = Double.parseDouble(strs1[1]); // 第一个地点的经度
        double latitude1 = Double.parseDouble(strs1[2]); // 第一个地点的纬度


        double longitude2 = Double.parseDouble(strs2[1]); // 第二个地点的经度
        double latitude2 = Double.parseDouble(strs2[2]); // 第二个地点的纬度

//        System.out.println(longitude1 + " " + longitude2 + " " + latitude1 + " " + latitude2);

        GeodesicData result = Geodesic.WGS84.Inverse(latitude1, longitude1, latitude2, longitude2);
//        System.out.println(result.s12);
        StringBuilder sb = new StringBuilder(strs1[0]).append(",").append(strs2[0]).append(",").append(result.s12);
        return sb.toString();
    }
}
