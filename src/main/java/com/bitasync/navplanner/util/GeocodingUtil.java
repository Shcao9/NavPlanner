package com.bitasync.navplanner.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;

/**
 * @author Lambda
 * @date 2023/11/23
 */
public class GeocodingUtil
{
    public static boolean geocode()
    {
        String filePath = "src/main/resources/csv/geolocation_data.csv";
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
            String[] locations = new String[70];
            for(int i = 1; i <= 63; ++i)
                locations[i] = "福建省福州市闽侯县福州大学学生公寓%d号".formatted(i);
            locations[64] = "福大玫瑰园餐厅(学府南路店)";
            locations[65] = "福大紫荆园餐厅";
            locations[66] = "福大丁香园餐厅";
            locations[67] = "福大海棠园餐厅";
            locations[68] = "岐安福大学生街(溪源宫路)";
            locations[69] = "福州大学快递服务中心";
            for(int i = 1; i < 2; ++i)
            {
                String json = HttpUtil.doGet("https://restapi.amap.com/v3/geocode/geo?key=f9bc2d2f2a35dd000e1ebe79a8b13c86&output=JSON&city=福州市&address=福州大学旗山校区博学苑B区学生公寓35号".formatted(locations[i]));
                System.out.println(locations[i] + " " + json);
				json = "{" + json.substring(json.indexOf("geocodes") + 12, json.lastIndexOf("}]}")) + "}";
				ObjectMapper mapper = new ObjectMapper();
				Map<String, Object> map = mapper.readValue(json, Map.class);
				StringBuilder sb = new StringBuilder();

				for (Object value : map.values())
					sb.append(value).append(",");
				if (!sb.isEmpty())
					sb.setLength(sb.length() - 1);
				writer.newLine();
				writer.write(String.valueOf(sb));
                Thread.sleep(200);
            }
            writer.close();
            System.out.println("数据已成功写入文件。");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
}
