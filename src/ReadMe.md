快速了解 Java Agent
@[toc]

#前言
jdk1.5以后引入了javaAgent技术，javaAgent是运行方法之前的拦截器。
我们利用javaAgent和ASM字节码技术，在JVM加载class二进制文件的时候，利用ASM动态的修改加载的class文件。

# 看一个例子

1. 创建`PreMainAgent`类

```java
public class PreMainAgent {

    /**
     * 在这个 premain 函数中，开发者可以进行对类的各种操作。
     * 1、agentArgs 是 premain 函数得到的程序参数，随同 “– javaagent”一起传入。与 main
     * 函数不同的是，
     * 这个参数是一个字符串而不是一个字符串数组，如果程序参数有多个，程序将自行解析这个字符串。
     * 2、Inst 是一个 java.lang.instrument.Instrumentation 的实例，由 JVM 自动传入。*
     * java.lang.instrument.Instrumentation 是 instrument 包中定义的一个接口，也是这
     * 个包的核心部分，
     * 集中了其中几乎所有的功能方法，例如类定义的转换和操作等等。
     **/


    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("=========premain方法执行1========");
        System.out.println(agentArgs);
    }

    /**
     * 如果不存在 premain(String agentArgs, Instrumentation inst)
     * 则会执行 premain(String agentArgs)
     */
    public static void premain(String agentArgs) {
        System.out.println("=========premain方法执行2========");
        System.out.println(agentArgs);
    }

}
```

> 类中提供两个静态方法，方法名均为premain，不能拼错

2. 在pom文件中添加打包插件

```xml

 <build>
        <plugins>
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <configuration>
                    <appendAssemblyId>false</appendAssemblyId>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <archive>
                        <!--自动添加META-INF/MANIFEST.MF -->
                        <manifest>
                            <addClasspath>true</addClasspath>
                        </manifest>
                        <manifestEntries>
                            <Premain-Class>com.java.agent.demo.PreMainAgent</Premain-Class>
                            <Agent-Class>com.java.agent.demo.PreMainAgent</Agent-Class>
                            <Can-Redefine-Classes>true</Can-Redefine-Classes>
                            <Can-Retransform-Classes>true</Can-Retransform-Classes>
                        </manifestEntries>
                    </archive>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

该插件会在自动生成META-INF/MANIFEST.MF文件时，帮我们添加agent相关的配置信息。

使用maven的package命令进行打包


3. 创建`Main`类

```java
public class Main {

    public static void main(String[] args) {
        System.out.println("hello world");
    }

}
```

编译`VM option`

```shell script
-javaagent:路径\java-agent-demo\target\java-agent-demo-0.0.1-SNAPSHOT.jar=helloagent
```

启动时加载javaagent，指向上一节中编译出来的java agent工程jar包地址，同时在最后追加参数helloagent。

运行MAIN方法，查看结果:

```shell script

=========premain方法执行1========
helloagent
hello world

```

可以看到java agent的代码优先于MAIN函数的方法运行，证明java agent运行正常。

*  如果有多个agent，会怎样执行呢，我们可以试下

```shell script

-javaagent:路径\java-agent-demo\target\java-agent-demo-0.0.1-SNAPSHOT.jar=helloagent
-javaagent:路径\java-agent-demo\target\java-agent-demo-0.0.1-SNAPSHOT.jar=helloagent2

```

可以看到输出:

```shell script

=========premain方法执行1========
helloagent
=========premain方法执行1========
helloagent2
hello world

```
可以看到多个agent是按照你配置的顺序执行的。


# 统计方法调用时间

Byte Buddy是开源的、基于Apache 2.0许可证的库，它致力于解决字节码操作和instrumentation API的复杂性。
Byte Buddy所声称的目标是将显式的字节码操作隐藏在一个类型安全的领域特定语言背后。
通过使用Byte Buddy，任何熟悉Java编程语言的人都有望非常容易地进行字节码操作。Byte
Buddy提供了额外的API来生成Java agent，可以轻松的增强我们已有的代码。
```xml

 <dependency>
                <groupId>net.bytebuddy</groupId>
                <artifactId>byte-buddy</artifactId>
                <version>1.9.2</version>
            </dependency>
            <dependency>
                <groupId>net.bytebuddy</groupId>
                <artifactId>byte-buddy-agent</artifactId>
                <version>1.9.2</version>
            </dependency>
```

修改PreMainAgent代码：

```java
public class PreMainAgent {

    /**
     * 在这个 premain 函数中，开发者可以进行对类的各种操作。
     * 1、agentArgs 是 premain 函数得到的程序参数，随同 “– javaagent”一起传入。与 main
     * 函数不同的是，
     * 这个参数是一个字符串而不是一个字符串数组，如果程序参数有多个，程序将自行解析这个字符串。
     * 2、Inst 是一个 java.lang.instrument.Instrumentation 的实例，由 JVM 自动传入。*
     * java.lang.instrument.Instrumentation 是 instrument 包中定义的一个接口，也是这
     * 个包的核心部分，
     * 集中了其中几乎所有的功能方法，例如类定义的转换和操作等等。
     **/


    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("=========premain方法执行1========");
        System.out.println(agentArgs);
        //创建一个转换器，转换器可以修改类的实现
        //ByteBuddy对java agent提供了转换器的实现，直接使用即可
        AgentBuilder.Transformer transformer = new AgentBuilder.Transformer() {
            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?>
                builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule
                javaModule) {
                return builder
                    // 拦截任意方法
                    .method(ElementMatchers.<MethodDescription>any())
                    // 拦截到的方法委托给TimeInterceptor
                    .intercept(MethodDelegation.to(MyInterceptor.class));
            }
        };
        new AgentBuilder // Byte Buddy专门有个AgentBuilder来处理Java Agent的场景
            .Default()
            // 根据包名前缀拦截类
            .type(ElementMatchers.nameStartsWith("com.java.agent.demo"))
            // 拦截到的类由transformer处理
            .transform(transformer)
            .installOn(inst);

    }

    /**
     * 如果不存在 premain(String agentArgs, Instrumentation inst)
     * 则会执行 premain(String agentArgs)
     */
    public static void premain(String agentArgs) {
        System.out.println("=========premain方法执行2========");
        System.out.println(agentArgs);
    }

}
```

先生成一个转换器，ByteBuddy提供了java agent专用的转换器。通过实现Transformer接口利用
builder对象来创建一个转换器。转换器可以配置拦截方法的格式，比如用名称，本例中拦截所有方
法，并定义一个拦截器类MyInterceptor。

创建完拦截器之后可以通过Byte Buddy的AgentBuilder建造者来构建一个agent对象。AgentBuilder可
以对指定的包名前缀来生效，同时需要指定转换器对象。

```java
public class MyInterceptor {

    @RuntimeType
    public static Object intercept(@Origin Method method,
        @SuperCall Callable<?> callable)
        throws Exception {
        long start = System.currentTimeMillis();
        try {
            //执行原方法
            return callable.call();
        } finally {
            //打印调用时长
            System.out.println(method.getName() + ":" +
                (System.currentTimeMillis() - start) + "ms");
        }
    }

}

```

MyInterceptor就是一个拦截器的实现，统计的调用的时长。参数中的method是反射出的方法对象，而
callable就是调用对象，可以通过callable.call（）方法来执行原方法。

重新打包，执行`maven package`命令。将`Main`类放置到`com.java.agent.demo` 包
下。修改代码内容为：


```java

public class Main {

    public static void main(String[] args) {
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("hello world");
    }

}

```

休眠1秒，使统计时长的演示效果更好一些。执行main方法之后显示结果：

我们在没有修改代码的情况下，利用java agent和Byte Buddy统计出了方法的时长，Skywalking的agent也是基于这些技术来实现统计调用时长。

```shell script

=========premain方法执行1========
helloagent
hello world
main:1001ms
```

