package br.pa.plus.genericservice;

public class BeanProperty {

  private String name;
  private Class<?> type;
  private Object value;
  
  public BeanProperty() {
    super();
  }

  public BeanProperty(String name, Class<?> type, Object value) {
    super();
    this.name = name;
    this.type = type;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return name + " " + type +" "+ value;
  }
  
}
