package br.pa.plus.genericservice;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import br.gov.pa.muiraquita.converter.annotations.PropertyMap;
import br.gov.pa.muiraquita.dto.AbstractDto;
import br.gov.pa.muiraquita.entity.AbstractEntity;
import br.gov.pa.muiraquita.reflection.Property;

import com.google.common.collect.ImmutableList;

public class QueryUtil {

  
  private static final ImmutableList<String> invalids = ImmutableList.of("class", "passaporte", "seamClient", "serialVersionUID");
  
  static {
    
    //invalids =ImmutableList.builder().add("").build();
  }
  
  public static String getQueryDefault(Class<? extends AbstractEntity> entityClass, Map<String, BeanProperty> map, AbstractDto dto) {
    
    String select = "SELECT o FROM " + entityClass.getSimpleName() + " o WHERE 0=0";
    
    StringBuffer where = new StringBuffer();
    for (String key : map.keySet()) {
      
      BeanProperty prop = map.get(key);
      
      if(prop.getType().equals(String.class)) {

        where.append(" AND UPPER(o." + prop.getName() + ") LIKE UPPER(:" + prop.getName() + ")" );
        
      } else {
        where.append(" AND o." + prop.getName() + " = :" + prop.getName() );
      }
      
    }
    String query = select + where.toString();
    return query;
  }
  
  public static String getQueryDefault(Class<? extends AbstractEntity> entityClass, Map<String, BeanProperty> map, AbstractDto dto, Integer campoOrdenacao) {
	    
	    String select = "SELECT o FROM " + entityClass.getSimpleName() + " o WHERE 0=0";
	    
	    StringBuffer where = new StringBuffer();
	    for (String key : map.keySet()) {
	      
	      BeanProperty prop = map.get(key);
	      
	      if(prop.getType().equals(String.class)) {

	        where.append(" AND UPPER(o." + prop.getName() + ") LIKE UPPER(:" + prop.getName() + ")" );
	        
	      } else {
	        where.append(" AND o." + prop.getName() + " = :" + prop.getName() );
	      }
	    }
	    if(campoOrdenacao != null) {
	      where.append(" ORDER BY " + campoOrdenacao);
	    }
	    String query = select + where.toString();
	    return query;
	  }
  
  public static void setParemeters(Query query, Map<String, BeanProperty> props) {
    
    for(String key : props.keySet()) {
      BeanProperty prop = props.get(key);
      if(prop.getType().equals(String.class)) {
        query.setParameter(prop.getName(), "%"+prop.getValue()+"%");
      } else {
        query.setParameter(prop.getName(), prop.getValue());
      }
    }
  }
  
  public static Map<String, BeanProperty> getNotNullValues(Class<? extends AbstractEntity> entityClass, AbstractDto dto) {

    try {
      
      List<String> entityFields = new ArrayList<String>();
      Class beanClass = getOriginalClassFromJavassist(entityClass);
      for (Field fd : beanClass.getDeclaredFields()) {
        entityFields.add(fd.getName());
      }
      
      HashMap<String, BeanProperty> mp = new HashMap<String, BeanProperty>();

      
      Field[] fds = getOriginalClassFromJavassist(dto).getDeclaredFields();
      for (Field fd : fds) {
        fd.setAccessible(true);
        if (!invalids.contains(fd.getName())) {
          
          Object value = Property.getPropertyValue(dto, fd.getName());
          
          if(value != null) {
            
            System.out.println(fd.getName()+"|"+fd.getType()+"|"+value);

            if(fd.isAnnotationPresent(PropertyMap.class)) { 
              String entityPropName = fd.getAnnotation(PropertyMap.class).propertyName();
              if(entityFields.contains(entityPropName)) {
                mp.put(fd.getName(), new BeanProperty(entityPropName, fd.getType(), value));
              }
            } else {
              if(entityFields.contains(fd.getName())) {
                mp.put(fd.getName(), new BeanProperty(fd.getName(), fd.getType(), value));
              }
            }
            
          }
        }
      }
      return mp;
    } catch (Throwable e) {
      e.printStackTrace();
      return null;
    }

  }
  
  
  public static Class getOriginalClassFromJavassist(Object bean) {
    return getOriginalClassFromJavassist(bean.getClass());
  }
  
  public static Class getOriginalClassFromJavassist(Class beanClass) {
    
    if (isProxy(beanClass)) {
      throw new RuntimeException("Seam cannot wrap JDK proxied IoC beans. Please use CGLib or Javassist proxying instead");
    }
    
    if (isWrapperClass(beanClass)) {
      beanClass = beanClass.getSuperclass();
    }
    
    return beanClass;
  }
  
  public static boolean isProxy(Class clazz) {
    return Proxy.isProxyClass(clazz);
  }
  
  public static boolean isWrapperClass(Class clazz) {
    return clazz != null && clazz.getName().contains("$$");
  }
  
}
