package dao;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.Cost;
import entity.Role;
import util.DBUtil;


/**
 * 处理大部分简单的SQL增删改查
 * 
 * @author tarena
 *
 */
abstract class GeneralSQL {

    /**
     * PreparedStatem的方法集合
     */
    private  Map<String,Method> methods;
    private  Map<String,Method> resultSetmethods;
    
    {
	methods=new HashMap<>();
	Method[] ms = PreparedStatement.class.getDeclaredMethods();
	for(Method m:ms){
	    methods.put(m.getName(), m);
	}
    }
    {
	resultSetmethods=new HashMap<>();
	Method[] ms = ResultSet.class.getDeclaredMethods();
	for(Method m:ms){
	    Class[] cls = m.getParameterTypes();
	    if(cls.length>0&&cls[0]==String.class){
	    resultSetmethods.put(m.getName(), m);
	    }
	}
    }
    
    public static void main(String...strings) throws SQLException{
	
//	GeneralSQL bdao = new GeneralSQL(){
//	    @SuppressWarnings("unchecked")
//	    @Override
//	    public  List<String> transGeneratedKeys(ResultSet rs) throws SQLException {
//		List<String> list=new ArrayList<>();
//		rs.next();
//		list.add(rs.getString(1));
//		return list;
//	    }};
//	    String sql=
//			"SELECT * FROM cost_myzzw ";
//	
//	List<Cost> list = bdao.work(Cost.class,true, sql,null);
//	for(Cost r:list){
//	    System.out.println(r);
//	}

    }

    /**
     * 通过传入的参数执行SQL，并返回List结果。
     * 执行插入操作的时候IRV需传入空的String数组或者需要返回的主键列名。
     * 执行其他操作时IRV可以指定为null。
     * @param hasReturn 是否有返回值，返回值包含两种情况 1、执行查询操作。2、执行插入操作，但是需要返回主键。
     * @param sql 执行的SQL语句。
     * @param IRV InsetResultValue 执行插入操作并需要返回的主键列，String数组。
     * @param params SQL语句中的参数，必须按顺序传入。
     * @return 指定类型的List集合。
     */
    @SuppressWarnings("rawtypes")
    public <T> List<T> work(Class cl1, boolean hasReturn, String sql, String[] IRV, Object... params) {
	Connection conn = null;
	try {
	    try {
		conn = DBUtil.getConnection();
		ResultSet rs = null;
		PreparedStatement ps = getPrepareStatement(sql, IRV, conn);
		evaluate(ps, params);
		if (hasReturn) {
		    if (IRV == null) {
			rs = ps.executeQuery();
			return transReToOb(rs, cl1);
		    } else {
			ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			return transGeneratedKeys(rs);
		    }
		} else {
		    ps.executeUpdate();
		    return null;
		}
	    }  finally {
		DBUtil.close(conn);
	    }
	} catch (SecurityException | IllegalArgumentException | IllegalAccessException
		    | InvocationTargetException e) {
		e.printStackTrace();
		throw new RuntimeException("", e);
	}catch (SQLException e) {
	    e.printStackTrace();
	    throw new RuntimeException("", e);
	}
    }

    private PreparedStatement getPrepareStatement(String sql, String[] IRV, Connection conn) throws SQLException {
	if (IRV == null) {
	    return conn.prepareStatement(sql);
	} else {
	    return conn.prepareStatement(sql, IRV);
	}
    }

    private void evaluate(PreparedStatement ps, Object... params)
	    throws IllegalAccessException, InvocationTargetException, SQLException {
	for (int i = 0; i < params.length; i++) {
	    if (params[i] != null) {
		Class<? extends Object> cl = params[i].getClass();
		String name = cl.getSimpleName();
		Method m = getMethod(cl, name);
		m.invoke(ps, i + 1, params[i]);
	    } else {
		ps.setObject(i + 1, params[i]);
	    }
	}
    }

    private Method getMethod(Class<? extends Object> cl, String name) {
	if (cl == Integer.class) {
	    return methods.get("setInt");
	} else {
	    return methods.get("set" + name);
	}
    }

    /**
     * 重写此方法，用于将特殊类型的Dao中，把ResultSet结果封装成相应类型实例集合。
     * @param rs SQL的执行结果集。
     * @return 指定类型的List集合。
     * @throws SQLException
     */
//    public abstract <T> List<T> transResultSetToObject(ResultSet rs) throws SQLException;
    
    /**
     * 重写此方法，用于封装插入操作的SQL执行后返回的主键值。可以封装成基本类型集合。
     * @param rs 用于插入操作的SQL返回的主键集。
     * @return 任意类型的List集合。
     * @throws SQLException
     */
    public abstract <T> List<T> transGeneratedKeys(ResultSet rs) throws SQLException;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> List<T> transReToOb(ResultSet rs, Class cl) {
	List<T> list = new ArrayList<>();
	try {
	    while (rs.next()) {
		Object obj = cl.newInstance();
		Field[] fileds = cl.getDeclaredFields();
		AccessibleObject.setAccessible(fileds, true);
		evaluateObj(rs, obj, fileds);
		list.add((T) obj);
	    }
	} catch (InstantiationException |
		 IllegalAccessException | 
		      SecurityException | 
		          SQLException e ) {
	    e.printStackTrace();
	} catch (IllegalArgumentException | 
		InvocationTargetException e) {
	    e.printStackTrace();
	}
	return list;
    }

    @SuppressWarnings("rawtypes")
    private void evaluateObj(ResultSet rs, Object obj, Field[] fileds)
	    throws IllegalAccessException, InvocationTargetException {
	for (Field field : fileds) {
	    StringBuffer name = new StringBuffer(field.getName());
	    parseFieldName(name);
	    Class cl1 = field.getType();
	    Method m = getMethod(cl1);
	    field.set(obj, m.invoke(rs, name.toString()));
	}
    }

    @SuppressWarnings("rawtypes")
    private Method getMethod(Class cl1) {
	if (cl1 == Integer.class) {
	    return resultSetmethods.get("getInt");
	} else {
	    return resultSetmethods.get("get" + cl1.getSimpleName());
	}
    }

    private void parseFieldName(StringBuffer name) {
	for (int i = 0; i < name.length(); i++) {
	    if (i > 0) {
		char ch = name.charAt(i);
		if (Character.isUpperCase(ch)) {
		    name.insert(i, "_");
		    i++;
		}
	    }
	}
    }
}
