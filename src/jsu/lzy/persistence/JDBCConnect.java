package jsu.lzy.persistence;
import java.lang.annotation.Documented;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;

@SuppressWarnings("all")
/**
 * 获取单例模式下的jdbc连接
 * 根目录下建立sql.properties文件，其中属性为driver、user、password、url
 * @author 大爷来了
 *
 */
public class JDBCConnect {
	// 单例模式当前类连接
	private static JDBCConnect instance = new JDBCConnect();
	// 连接驱动
	private String driver;
	// 用户名
	private String user;
	// 用户密码
	private String password;
	// 链接地址
	private String url;
	// 连接
	private Connection conn = null;

	/**
	 * 保证单次获取文件属性
	 */
	private JDBCConnect() {
		try {
			// 获取配置文件位置
			ResourceBundle rb = ResourceBundle.getBundle("sql");
			driver = rb.getString("driver");
			user = rb.getString("user");
			password = rb.getString("password");
			url = rb.getString("url");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	/**
	 * 单例模式获取对象
	 * @return
	 */
	public static JDBCConnect getInstance() {
		return instance;
	}

	/**
	 * 获取数据库连接
	 * 
	 * @return
	 */
	public Connection getConn() {
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, password);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return conn;
	}

	/**
	 * 关闭数据库连接
	 */
	public void close() {
		try {
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
