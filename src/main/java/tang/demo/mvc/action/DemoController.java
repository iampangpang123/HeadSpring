package tang.demo.mvc.action;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tang.demo.service.DemoService;
import tang.mvcframework.annotation.TangAutowired;
import tang.mvcframework.annotation.TangController;
import tang.mvcframework.annotation.TangRequestMapping;
import tang.mvcframework.annotation.TangRequestParam;

@TangController
public class DemoController {

	@TangAutowired
	private DemoService demoService;

	@TangRequestMapping("demo.action")
	public void query(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("utf-8");
		response.setContentType("text/html;charset=utf-8");
		
		
		
		String name=request.getParameter("name");
		System.out.println("传递的name是"+name);
	  String result = demoService.get(name);
		try {
			response.getWriter().write("请求参数为：：：："+result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
