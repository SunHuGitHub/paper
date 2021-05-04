package com.tiger.paper;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Tiger
 * @date 2021/5/2 15:44
 */
public class SPIDemo {
    public static void main(String[] args) {
        ServiceLoader<LogService> load = ServiceLoader.load(LogService.class);
        Iterator<LogService> it = load.iterator();
        while (it.hasNext()){
            it.next().print();
        }
    }
}
