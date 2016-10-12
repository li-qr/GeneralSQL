# GeneralSQL
##使用此类以简化jdbc的操作
详细描述：blog.leezw.net/144.lee
当需要使用jdbc是，将DAO类继承此虚拟类。
执行SQL语句时，先定义SQL语句。然后调用work()方法。
当需要获得插入时的主键时，需要重写此虚方法：
public abstract <T> List<T> transGeneratedKeys(ResultSet rs) throws SQLException;
此时在执行work时就可以封装返回值。



