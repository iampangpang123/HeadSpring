package tang.demo.service.impl;

import tang.demo.service.DemoService;
import tang.mvcframework.annotation.TangService;
@TangService
public class DemoServiceImpl  implements DemoService{

	public String get(String name) {
		System.out.println("我是service:得到了controller传递过来的值：：：："+name);
		return "我是service:得到了controller传递过来的值：：：："+name;
	}

}
