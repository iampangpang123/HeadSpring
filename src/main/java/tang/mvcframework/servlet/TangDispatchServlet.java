package tang.mvcframework.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tang.mvcframework.annotation.TangAutowired;
import tang.mvcframework.annotation.TangController;
import tang.mvcframework.annotation.TangRequestMapping;
import tang.mvcframework.annotation.TangService;

public class TangDispatchServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// 保存配置信息
	private static Properties props = new Properties();
	// 保存扫描的类
	private List<String> classNames = new ArrayList<String>();
	// ioc容器类---Key是类实例的名字，value是类的实例对象
	Map<String, Object> ioc = new HashMap<String, Object>();
//定义一个处理器映射器集合。封装方法与映射路径---------key是url,method是方法
	Map<String, Method> handMapping =new HashMap<String, Method>();
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
			doDispatch(req, resp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			resp.getWriter().write("500错误");;
		e.printStackTrace();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 根据url,调用不同方法
		try {
			doDispatch(req, resp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			resp.getWriter().write("500错误"+e.getMessage());
		}
	}

	/**
	 ** 
	 * @Description:根据url,调用不同方法
	 * @param: @param req
	 * @param: @param resp
	 * @return: void
	 * @throws Exception 
	 */
	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {

		if (handMapping.isEmpty()) {
			return;//如国这个集合是空的，下面就没有必要执行了，映射不上。
		}
		String requestURI = req.getRequestURI();//得到请求路径---绝对路径--包含项目路径
		System.out.println("请求路径"+requestURI);
		String contextPath=req.getContextPath();//这是项目路径
		System.out.println("项目路径路径"+contextPath);
		requestURI=requestURI.replace(contextPath, "").replaceAll("/+", "/");//把绝对路径里面的相对路径替换成空，就是对应集合里面封装的handmapping 集合里面路径了
		System.out.println("修改后的请求路径："+requestURI);
		//开始根据请求路径掉用方法了
		if(!handMapping.containsKey(requestURI)) {
			resp.getWriter().write("404");
			return;
		}
		//使用一个集合，request封装请求带来的参数与值
		Map<String,String[]> requestMapParms=req.getParameterMap();
		
		
		//根据url得到方法
		Method method = this.handMapping.get(requestURI);
		
		//通过反射运行方法需要： method.invoke(oject,parms)设置方法的实例对象，参数
        String beanName = method.getDeclaringClass().getSimpleName();//得到方法所在的类名，我们一会从容器里面取
         //但是容器里面是小写，所以我们把类名首字符转成小写
        beanName=lowerFisterCase(beanName);
        
        //它们以声明顺序表示由此Method对象表示的方法的形式参数类型。如果底层方法没有参数，则返回长度为0的数组。
		 Class<?>[] methodTypes = method.getParameterTypes();
		//用一个数组保存实参，大小与形参对应
		 Object[] objects=new Object[methodTypes.length];
		 //遍历形参列表
		 for (int i = 0; i < methodTypes.length; i++) {//为什么用传递遍历，因为要保证设置值的时候与参数对应
			Class<?> parameterType = methodTypes[i];//得到方法参数的类型
			if (parameterType==HttpServletRequest.class) {//如果是request
				objects[i]=req;//给实参设置值保存在object数组中
				continue;//这个if下面不在判断执行了
			}else if (parameterType==HttpServletResponse.class) {
				objects[i]=resp;
				continue;
			}else if(parameterType==String.class) {
				for (Map.Entry<String, String[]>  param : requestMapParms.entrySet()) {
					//将数组转字符串，去掉数组里面的括号之类的
					String value=Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replace(",\\s", ",");
					objects[i]=value;
					
				}				
				//你要想封装其他参数，比如String 类型的其他类型的（就是RequestParm注解功能，你自己在下面加及格else if判断）
				//为了方便，我们只封装request response.通过request也能调取到传递过来的参数
				//虽然我们写了但是我们controller没用TangRequestParm注解，这个else if只是参考，指导怎么写
			}
			 
		}
		
		//method.invoke(this.ioc.get(beanName), objects);//可变参数，放object数组也行
		method.invoke(this.ioc.get(beanName), req,resp);
		
	}

	@Override
	public void init(ServletConfig config) throws ServletException {//项目启动时候初始化调用
		// 1.加载配置文件
		// getInitParameter ：就是独取init标签写的properties路径
//		 <init-param>
//		 <param-name>contextConfigLocation</param-name>
//	   	<param-value>classpath:application.properties</param-value>
//		</init-param>
		System.out.println("初始化开始");
		loadConfig(config.getInitParameter("contextConfigLocation"));
		// 2.扫描相关类
		scannerClass(props.getProperty("scanPackage"));
		// 3.把扫描出来的相关类实例化，并且存入到ioc容器
		instanceClass();
		// 4.完成自动赋值，依赖注入
		autowiredDI();
		// 5.初始化HanderMapping:处理器映射器：处理请求
		initHandlerMapping();

	}

	/**
	 ** 
	 * @Description:/ 1.加载配置文件
	 * @param: @param location
	 * @return: void
	 */
	private void loadConfig(String location) {
		System.out.println("ServletConfig.getInitParameter(\"contextConfigLocation\"):路径为"+location);//路径为application.properties
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(location);//这个方法是得到类路径下的资源
		try {
			props.load(in);// 得到了properties,然後就可以通过prop.getProperties("name")得到文件里面的值
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 ** 
	 * @Description:// 2.扫描相关类
	 * @param:
	 * @return: void
	 */
	private void scannerClass(String scanPackage) {
		// String
		// scanPackage=props.getProperty("scanPackage");//得到配置文件application.properties里面的scanPackage的值
		URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));// 得到文件绝对路径
		// url.getFile() :返回文件夹路径
		// file ：该类主要用于文件和目录的创建、文件的查找和文件的删除等。
		File classDir = new File(url.getFile());// 通过将给定路径名字符串转换成抽象路径名来创建一个新 File 实例：
		for (File file : classDir.listFiles()) {
			if (file.isDirectory()) {// 如果file还是一个文件夹，继续递归遍历这个文件夹
				scannerClass(scanPackage + "." + file.getName());// 调用自身方法实现递归
			} else {// 不国不是文件夹，说明是类路径了，把每个类路径放入到List中
				classNames.add(scanPackage + "." + file.getName().replace(".class", "").trim());
			}
		}

	}

	/**
	 ** 
	 * @Description:初始化集合 classNames(放的是class的类路径) 里面每一个类实例，然后保存在Map ioc集合中
	 * @param:
	 * @return: void
	 */
	private void instanceClass() {
		if (classNames.size() == 0) {
			return;// 就是下面的所有代码不执行了
		}
		try {
			for (String className : classNames) {
				Class<?> clazz = Class.forName(className);// 根据每个类路径根据反射生成类的实例
				if (clazz.isAnnotationPresent(TangController.class)) {
					String beanName = clazz.getSimpleName();// 得到类在底层的简称；默认的类名首字母是大写的。由于spring的ioc生成的类的首字符小写，所以我们要写个方法转化一下
					String newBeanName = lowerFisterCase(beanName);
					ioc.put(newBeanName, clazz.newInstance());
					System.out.println("ioc容器：：：key为"+newBeanName+"key为：："+clazz.newInstance());
				} else if (clazz.isAnnotationPresent(TangService.class)) {
					// 使用service注解可能用户会自定别名--优先使用自定义别名，否者使用默认的：当然controller也会，我们就拿service举例子
					TangService tangService = clazz.getAnnotation(TangService.class);
					String beanName = tangService.value();// 得到用户自定义的值
					if (!"".equals(beanName.trim())) {// 不是空的就说明用户自定义了
						ioc.put(beanName, clazz.newInstance());
						System.out.println("ioc容器：：：key为"+beanName+"value为：："+clazz.newInstance());
						continue;
					}
					// 不然就使用默认的-----由于我们在Controller中使用的是接口引用service.使用实现类的名字不行。需要是接口
					// 所以将接口全称（而不是类的全称作为key） 作为key,实例类作为值
					Class<?>[] interfaces = clazz.getInterfaces();// 得到类的所有接口
					for (Class<?> i : interfaces) {
						if (ioc.containsKey(i.getSimpleName())) {
							throw new Exception("该接口已经创建了一个实例");
						}
						ioc.put(lowerFisterCase(i.getSimpleName()), clazz.newInstance());//类名转小写
						System.out.println("ioc容器：：：key为"+lowerFisterCase(i.getSimpleName())+"value为：："+clazz.newInstance());
						
					}
				} else {
					// 直接忽略
					continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 ** 
	 * @Description:依赖注入
	 * @param:
	 * @return: void
	 */
	private void autowiredDI() {
		if (ioc.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> entry : ioc.entrySet()) {// 遍历ioc容器
			// 给这个类上的字段赋值
			Field[] fields = entry.getValue().getClass().getDeclaredFields();// 得到类的所有属性
			for (Field field : fields) {// 判断属性是否有TangAutowird注解
				if (!field.isAnnotationPresent(TangAutowired.class)) {
					continue;
				}
				// 不然就是对字段赋值
				TangAutowired tangAutowired = field.getAnnotation(TangAutowired.class);// 得到字段上的注解
				String annoBeanName = tangAutowired.value().trim();// 得到字段上注解的值
				if (annoBeanName.equals("")) {// 因为Tangautowired注解value方法默认值是“”,说明用户没自定义名字
					// 我们根据类型注入，下面方法相当于返回类的名字
					annoBeanName = field.getType().getName();//这是全类名
					System.out.println("依赖注入annoBeanName值"+annoBeanName);
				}
				//这个filed就是要操作的字段：即头上带有TangAutowired注解的字段
				field.setAccessible(true);// 设置私有属性访问权限
				try {
					System.out.println(ioc.values());
//					ioc容器：：：key为demoControllerkey为：：tang.demo.mvc.action.DemoController@133fad72
//					ioc容器：：：key为demoServicevalue为：：tang.demo.service.impl.DemoServiceImpl@2fce2258
//					依赖注入annoBeanName值tang.demo.service.DemoService，但是    我們没这个key 啊
//					依赖注入entry.getValue()的值tang.demo.mvc.action.DemoController@1f5fc1f0
					System.out.println("依赖注入entry.getValue()的值"+entry.getValue());
					// 用反射给字段赋值----field(object,"wangwu")。object就是这个entry对象，
				//	field.set(entry.getValue(), ioc.get(annoBeanName));
					System.out.println(field.getName());//demoService，这个才是字段名
					field.set(entry.getValue(), ioc.get(field.getName()));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

	}

	/**
	 ** 
	 * @Description:初始化处理器映射器，绑定requestMapping与方法
	 * @param:
	 * @return: void
	 */
	private void initHandlerMapping() {
		if (ioc.size() == 0) {
			return;
		}
		for (Map.Entry<String, Object> entry : ioc.entrySet()) {
			Class<?> clazz = entry.getValue().getClass();// 得到容器里面的clazz实例
			String classUrl = "";
			String methodUrl="";
			String url="";
			// 1.获取类上的requestMapping注解
			if (clazz.isAnnotationPresent(TangRequestMapping.class)) {// 判断是否存在这个给注解
				TangRequestMapping tangRequestMappingOnClass = clazz.getAnnotation(TangRequestMapping.class);// 得到这个注解
				classUrl = tangRequestMappingOnClass.value();// 得到注解的值；这个值应该是个url路径
			}

			Method[] methods=clazz.getMethods();
			for (Method method : methods) {
				if(!method.isAnnotationPresent(TangRequestMapping.class)) {
					continue;
				}
				//得到方法上的url
				methodUrl=method.getAnnotation(TangRequestMapping.class).value();
				//根据类上的url+方法的url拼接成完成的url-------小处理，防止用户写多个斜杠
				url=classUrl+"/"+methodUrl.replace("/+", "/");
				//把url，已经url对应的方法，放在一个map集合中，为后面处理器映射器映射做准备
				handMapping.put(url, method);
				System.out.println("初始化hanlerMapping+++"+"url为：："+url+"method为"+method);
			}
		}

	}

	/**
	 ** 
	 * @Description:字符串首字母小写
	 * @param: @param beanName
	 * @param: @return
	 * @return: String
	 */
	private String lowerFisterCase(String beanName) {
		char[] chars = beanName.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}

}
