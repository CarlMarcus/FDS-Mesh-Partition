package Rough;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CountThin {

	private static List<Integer> readFile(String filepath) throws IOException {
        /*
         * @param fds file path
         * @return fds文件每行string的集合 List
         *  读取fds文件，以行为单位存入一个ArrayList内
         */
        List<Integer> list = new ArrayList<>();
        String encoding = "GBK";
        File file = new File(filepath);
        if (file.isFile() && file.exists()) {
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
            BufferedReader br = new BufferedReader(read);
            String str = null;
            int count = 0;
            while ((str=br.readLine())!=null) {
            	count++;
            	if (str.length()>5 && str.substring(1, 5).equalsIgnoreCase("OBST")) {
            		Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
            		Matcher matcher = pattern.matcher(str);
            		int j = 0;
                    double[] obst = new double[7];
                    while (matcher.find()) {
                        obst[j++]=Float.valueOf(matcher.group().trim()).doubleValue();                        
                    }
                    
                    if (Math.abs(obst[0]-obst[1])<0.35 || Math.abs(obst[2]-obst[3])<0.35 || Math.abs(obst[4]-obst[5])<0.35) {
                    	list.add(count);
                    }
            	}
            }
        }
		return list;     
    }
	
	public static void main(String[] args) throws IOException {
		String str = "C:\\Users\\gongz\\Desktop\\16all\\16all.fds";
		List<Integer> list = readFile(str);
		for(int i=0; i<list.size(); i++) {
			System.out.println(list.get(i));
		}
		System.out.println(list.size());
	}

}
