## sprouts-bean-mapper

### sprouts-bean-mapper解决什么问题

不同服务层都有自己服务层独有的bean, 例如DTO, BO, DO等等.

这些Bean中的元素大多数都是相同的,但是由于各种原因里面的属性名称可能会有细微差别.

比如 cno 与 customNo 等价, password和pwd等价, 等等.

通常我们会使用String的BeanUtils来转换这些对象. 而这些不同属性名的属性会通过其他手段进行复制值.

sprouts-bean-mapper解决了bean中不同属性名称映射的问题. 通过sprouts-bean-mapper的处理后,可以直接使用String的BeanUtils来复制对象,不同属性名的属性赋值不用再单独处理.


### 使用方式

(1) 添加Maven引用

```xml
<dependency>
  <groupId>com.github.gs618</groupId>
  <artifactId>sprouts-bean-mapper</artifactId>
  <version>1.0.0</version>
  <scope>provided</scope>
</dependency>
```

(2) 在目标bean的属性上添加@BeanMapper注解

```java
public class ObjectC {

    @BeanMapper(targets = {"objectA"})
    private ObjectA objA;

    @BeanMapper(targets = {"stringField", "str"})
    private String strField;

    @BeanMapper(targets = {"intField"})
    private int iField;

}
```

(3) 使用Spring的BeanUtils复制对象即可

(4) Sample请参考下面链接

[Sample](https://github.com/gs618/sprouts-bean-mapper-sample)



### sprouts-bean-mapper 能做什么

sprouts-bean-mapper只是一个简单的项目.所以它的所有功能就只是做不同属性名称的映射, 所以还是需要配合spring的BeanUtils进行复制对象.



### 注意事项

(1) 单向映射, 如果需要双向映射, 需要两个类之间使用注解相互标注

(2) 无法分辨多个重复的目标, 即同一个类中, 不同的属性都指向同一个源时, 编译器会报错(这个是因为实现原理的原因) 

(3) 强入侵, class文件将被修改
