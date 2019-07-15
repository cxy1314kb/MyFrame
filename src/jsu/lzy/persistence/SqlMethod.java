package jsu.lzy.persistence;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 公用的sql基本操作方法：包括根据主键增、删、改、查，根据对象增、删、改、查。
 * 其他复杂方法在当前方法内如果不适合，建议调用当前类的getConnect后获取单例模式数据库连接后使用传统jdbc模式开发。
 * 当前版本为简易版本、后期会加入复杂型xml开发模式。
 * 简易版本中，适合小型项目使用，为方便检测是否为当前工具类出现问题，会打印每条执行sql,后期会加入log方式，强迫症者可直接修改源码。
 * 当前类没有将可公共的方法抽取出来，仅为更好学习使用，其他开发者可以参考后自行开发。
 * 当前为基于注解+反射实现，还有一种模式为单注解模式：无法将表属性和POJO属性对应，只有当表属性和POJO属性一致可使用，此种模式未键入当前类。
 * 个人开发，不喜勿喷，有不规范处请见谅。
 * 如要查看源码，建议其他开发者对getList方法开始进行观看。
 * 使用方法：建立一个类extends当前类并实现一个具体的POJO，如：class TestDao extends SqlMethod<TestPOJO>:其中TestPOJO为POJO也即是bean对象。
 * 例：POJO类在jar包中查看（jar中包带一个简易使用类）
 * TestPOJO TestPOJO = new TestPOJO();
 * TestPOJO.setName("小黄");
 * TestDao testDao = new TestDao();
 * testDao.updateByKey(TestPOJO, 1);
 * 结果:
 * UPDATE student SET user_name='小黄' WHERE user_age=1
 * 其他：数据库连接配置文件说明：
 * 根目录下建立sql.properties文件，其中属性为driver、user、password、url。
 * 依赖：数据库连接包，请大家自行导入
 * @author 大爷来了
 * @create 2019-07-12 20:00
 * @param <T>
 * 
 */
@SuppressWarnings("all")
public abstract class SqlMethod<T> {
	/**
	 * 查询多条记录 limits[0]要查询的页数，limits[1]查询记录数，其他索引无效，limits无参或参数小于2代表不使用分页查询。
	 * t:传入判断条件
	 * @param t：查询对象
	 * @param limits：分页
	 * @return
	 */
	public List getList(T t, int... limits) {
		// 获取数据库连接conn
		Connection conn = getConnect();
		// 获取传入t泛型类c1
		Class<T> c1 = (Class<T>) t.getClass();
		// 反射c1的所有属性，不包括继承
		Field[] declaredFields = c1.getDeclaredFields();
		// 初始化数据库查询语句，用于字符串拼接，代替*
		StringBuffer sqlcoloum = new StringBuffer();
		// 非空属性放在where后
		StringBuffer sqlColoumNotNull = new StringBuffer();
		// 占位符集合
		List<Object> placeholder = new ArrayList();
		// 判断传入是否有非空属性标准，默认无
		boolean isHaveColoum = false;
		/*
		 * 当前for循环作用 获取c1类对象的所有ColoumName注解对象，以此来对应sql表中的字段
		 */
		for (Field field : declaredFields) {
			// 取消安全检测
			field.setAccessible(true);
			// 获取coloumName.class注解对象
			coloumName annotation = field.getAnnotation(coloumName.class);
			// 获取列明
			String coloumNmae = annotation.name();
			// 拼接要查询的列，代替*
			sqlcoloum.append(coloumNmae + ",");
			try {
				// 如果值为空则为默认不以此为查询条件,注意int为0时默认为空-加：如果是int型且值为0则也为空
				if (null != field.get(t)) {
					// 如要添加其他类型为空判断，建议如下修改
					// 如果是int类型且为0则也算为空
					if (field.getType().toString().equals("int") ? 0 == (int) field.get(t) : false)
						continue;
					// 以有非空属性
					isHaveColoum = true;
					sqlColoumNotNull.append(coloumNmae + "=");
					sqlColoumNotNull.append("? and ");
					placeholder.add(field.get(t));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 删除列名后一个,号
		sqlcoloum.deleteCharAt(sqlcoloum.length() - 1);
		// 如果有非空属性，删除后的and符号以及2个空格
		if (isHaveColoum) {
			sqlColoumNotNull.delete(sqlColoumNotNull.length() - 5, sqlColoumNotNull.length());
		}
		// 获取传入对象对应表名,
		String tableName = c1.getAnnotation(TableName.class).value();
		// 拼接最后sql帶占位符
		String sql = "select " + sqlcoloum.toString() + " from " + tableName + " where " + sqlColoumNotNull.toString();
		// 拼接分页查询
		if (limits.length > 1) {
			sql += " limit " + limits[0] * limits[1] + "," + limits[1];
		}
		/**
		 * 第二步，执行sql
		 */
		PreparedStatement ps = null;
		ResultSet rs = null;
		// 定义返回值List
		List<T> list = new ArrayList();
		try {
			ps = conn.prepareStatement(sql);
			// 占位符入栈
			for (int i = 0; i < placeholder.size(); i++) {
				ps.setObject(i + 1, placeholder.get(i));
			}
			// 执行sql语句
			rs = ps.executeQuery();
			while (rs.next()) {
				// 重新生成对象
				T t1 = c1.newInstance();
				// 反射将所有查询到的属性值加入泛型对象中，通过rs.getObject(列名)
				for (Field field : declaredFields) {
					c1.getDeclaredConstructors();
					String coloumName = field.getAnnotation(coloumName.class).name();
					field.set(t1, rs.getObject(coloumName));
				}
				// 加入List
				list.add(t1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 关闭
		closeAll(rs, ps, conn);
		return list;
	}

	/**
	 * 根据key获取单条记录
	 * 
	 * @param key：主键
	 * @return
	 */
	public T getBykey(int key) {
		T newInstance = null;
		Class<T> tClass = getTClass();
		Connection conn = getConnect();
		String tableName = tClass.getAnnotation(TableName.class).value();
		// key列名
		String keyColoumName = null;
		Field[] declaredFields = tClass.getDeclaredFields();
		for (Field field : declaredFields) {
			coloumName annotation = field.getAnnotation(coloumName.class);
			if (annotation.isId()) {
				keyColoumName = annotation.name();
				break;
			}
		}
		String sql = "select * FROM " + tableName + " WHERE " + keyColoumName + "=" + key;
		Statement s = null;
		ResultSet rs = null;
		try {
			s = conn.createStatement();
			rs = s.executeQuery(sql);
			if (rs.next()) {
				newInstance = tClass.newInstance();
				for (Field field : declaredFields) {
					field.setAccessible(true);
					coloumName annotation = field.getAnnotation(coloumName.class);
					field.set(newInstance, rs.getObject(annotation.name()));
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		closeAll(rs, s, conn);
		return newInstance;

	}

	/**
	 * 添加记录，使用占位符方法添加
	 * 
	 * @param t：添加对象
	 * @return
	 */
	public int add(T t) {
		int re = 0;
		Class<?> class1 = t.getClass();
		StringBuffer coloumNames = new StringBuffer();
		coloumNames.append("insert into " + class1.getAnnotation(TableName.class).value() + "(");
		StringBuffer sqlColoumNotNull = new StringBuffer();
		sqlColoumNotNull.append(" values (");
		Field[] declaredFields = class1.getDeclaredFields();
		// 占位符集合
		List<Object> placeholder = new ArrayList();
		for (Field field : declaredFields) {
			// 取消安全检测
			field.setAccessible(true);
			// 获取coloumName.class注解对象
			coloumName annotation = field.getAnnotation(coloumName.class);
			// 获取列明
			String coloumNmae = annotation.name();
			// 拼接要查询的列，代替*
			try {
				// 如果值为空则为默认不以此为查询条件,注意int为0时默认为空-加：如果是int型且值为0则也为空
				if (null != field.get(t)) {
					// 如要添加其他类型为空判断，建议如下修改
					// 如果是int类型且为0则也算为空
					if (field.getType().toString().equals("int") ? 0 == (int) field.get(t) : false)
						continue;
					// 以有非空属性
					coloumNames.append(coloumNmae + ",");
					sqlColoumNotNull.append("?,");
					placeholder.add(field.get(t));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		coloumNames.deleteCharAt(coloumNames.length() - 1);
		coloumNames.append(")");
		sqlColoumNotNull.deleteCharAt(sqlColoumNotNull.length() - 1);
		sqlColoumNotNull.append(")");
		// 最终sql
		String sql = coloumNames.append(sqlColoumNotNull).toString();
		// 第2步执行sql
		Connection conn = getConnect();
		PreparedStatement ps = null;
		// 定义返回值List
		List<T> list = new ArrayList();
		try {
			ps = conn.prepareStatement(sql);
			// 占位符入栈
			for (int i = 0; i < placeholder.size(); i++) {
				ps.setObject(i + 1, placeholder.get(i));
			}
			// 执行sql语句
			System.out.println(ps.toString());
			re = ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		closeAll(null, ps, conn);
		return re;
	}

	/**
	 * 根据key删除
	 * 
	 * @param key：主键
	 * @return
	 */
	public boolean deleteByKet(int key) {
		Class<T> tClass = getTClass();
		// 默认删除失败
		int re = 0;
		// 数据库连接conn
		Connection conn = getConnect();
		// 表名
		String tableName = tClass.getAnnotation(TableName.class).value();
		// key列名
		String keyColoumName = null;
		Field[] declaredFields = tClass.getDeclaredFields();
		for (Field field : declaredFields) {
			coloumName annotation = field.getAnnotation(coloumName.class);
			if (annotation.isId()) {
				keyColoumName = annotation.name();
				break;
			}
		}
		String sql = "DELETE FROM " + tableName + " WHERE " + keyColoumName + "=" + key;
		Statement s = null;
		// 定义返回值List
		try {
			s = conn.createStatement();
			re = s.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return re>0?true:false;
	}
	/**
	 * 暂未开发，预计内容，根据传入t对象删除内容
	 * @param t：删除对象
	 * @return
	 */
	@Deprecated
	public int deleteByPOJO(T t) {
		return 0;
		
	}
	/**
	* 暂未开发，预计内容，根据传入t1对象，修改成传入对象t中的内容
	 * @param t：修改对象
	 * @param t1：判断对象
	 * @return
	 */
	@Deprecated
	public int updateByPOJO(T t,T t1) {
		return 0;
		
	}
	/**
	 * 根据key主键来修改元素。修改内容为传入对象中的内容，对象属性值为空代表sql内当前列不修改。
	 * @param t：修改对象
	 * @param key：主键
	 * @return
	 */
	public boolean updateByKey(T t,int key){
		Class<T> tClass = getTClass();
		// 默认删除失败
		int re = 0;
		// 表名
		String tableName = tClass.getAnnotation(TableName.class).value();
		// key列名
		String keyColoumName = null;
		//获取主键的列明
		Field[] declaredFields = tClass.getDeclaredFields();
		for (Field field : declaredFields) {
			coloumName annotation = field.getAnnotation(coloumName.class);
			if (annotation.isId()) {
				keyColoumName = annotation.name();
				break;
			}
		}
		//拼接set字段属性
		StringBuffer setColoums=new StringBuffer();
		List<Object> setColoumsNums=new ArrayList<>();
		Field[] fields = t.getClass().getDeclaredFields();
		boolean isHaveColoum=false;
		for (Field field : fields) {
			// 取消安全检测
			field.setAccessible(true);
			// 获取coloumName.class注解对象
			coloumName annotation = field.getAnnotation(coloumName.class);
			// 获取列名
			String coloumNmae = annotation.name();
			// 拼接要查询的列，代替*
			try {
				// 如果值为空则为默认不以此为查询条件,注意int为0时默认为空-加：如果是int型且值为0则也为空
				if (null != field.get(t)) {
					// 如要添加其他类型为空判断，建议如下修改
					// 如果是int类型且为0则也算为空
					if (field.getType().toString().equals("int") ? 0 == (int) field.get(t) : false)
						continue;
					// 以有非空属性
					isHaveColoum = true;
					setColoums.append(coloumNmae + "=");
					setColoums.append("?,");
					setColoumsNums.add(field.get(t));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		setColoums.deleteCharAt(setColoums.length()-1);
		String sql = "UPDATE "+tableName+" SET " +setColoums+ " WHERE " + keyColoumName + "=" + key;
		//执行占位符sql
		Connection conn = getConnect();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			// 占位符入栈
			for (int i = 0; i < setColoumsNums.size(); i++) {
				ps.setObject(i + 1, setColoumsNums.get(i));
			}
			// 执行sql语句
			System.out.println(ps.toString());
			re = ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return re>0?true:false;
		
	}
	// 以下是工具方法
	/**
	 * 获取数据库连接,提供外部接口调用，重写其他复杂sql
	 * 
	 * @return
	 */
	public Connection getConnect() {
		JDBCConnect jdbc = JDBCConnect.getInstance();
		Connection Conn = jdbc.getConn();
		return Conn;
	}

	/**
	 * 关闭所有连接
	 * 
	 * @param rs
	 * @param s
	 * @param conn
	 */
	private void closeAll(ResultSet rs, Statement s, Connection conn) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (s != null) {
			try {
				s.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 获取泛型的类对象
	 * 
	 * @return
	 */
	private Class<T> getTClass() {
		ParameterizedType type = (ParameterizedType) this.getClass().getGenericSuperclass();
		Class<T> tClass = (Class<T>) type.getActualTypeArguments()[0];
		return tClass;
	}
}
