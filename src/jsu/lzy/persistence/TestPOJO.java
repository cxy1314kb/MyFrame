package jsu.lzy.persistence;
//映射数据库表名注解
@TableName("student")
class TestPOJO {
	//name对应数据库表列明注解，isId代表是否为主键注解
	@coloumName(name = "user_age", isId = true)
	private int age;
	@coloumName(name = "user_name")
	private String name;
	// 省略getset方法
}
