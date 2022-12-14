---
title: XML DOM API
---

<!--  TODO内容:DOM <=> PSI,转到符号,编辑器装订图标 --> DOM  -->


##摘要


This article is intended for plugin writers who create custom web server integrations, or some UI for easy XML editing. It describes the *Document Object Model* (DOM) in IntelliJ Platform --- an easy way to work with DTD or Schema-based XML models.
The following topics will be covered: working with DOM itself (reading/writing tags content, attributes, and subtags) and easy XML editing in the UI by connecting UI to DOM.

假设读者熟悉Java 5,Swing,IntelliJ Platform XML PSI(类[`XmlTag`](upsource:///xml/xml-psi-api/src/com/intellij/psi/xml/XmlTag). 
java),[`XmlFile`](upsource:///xml/xml-psi-api/src/com/intellij/psi/xml/XmlFile.java),[`XmlTagValue`](upsource:///xml/xml-psi-api/src/com/intellij/psi/xml/XmlTagValue.java)等),IntelliJ平台插件开发基础知识(应用程序和项目组件,文件编辑器).


## 介绍


那么,如何使用IntelliJ平台插件中的XML进行操作？
通常,必须使用`XmlFile`,获取其根标记,然后按路径查找所需的子标记.
该路径由标记名称组成,每个标记名称都是一个字符串.
在任何地方打字都很乏味且容易出错.
假设您有以下XML:


```xml
<root>
    <foo>
        <bar>42</bar>
        <bar>239</bar>
    </foo>
</root>
```

假设您要读取第二个bar元素的内容,即“239”.


创建链式调用是不正确的


```java
file.getDocument().getRootTag().findFirstSubTag("foo").
findSubTags("bar")[1].getValue().getTrimmedText()
```
because each call here may return `null`.

So the code would probably look like this: 
                
```java
XmlFile file = ...;
final XmlDocument document = file.getDocument();
if (document != null) {
    final XmlTag rootTag = document.getRootTag();
    if (rootTag != null) {
        final XmlTag foo = rootTag.findFirstSubTag("foo");
        if (foo != null) {
            final XmlTag[] bars = foo.findSubTags("bar");
            if (bars.length > 1) {
                String s = bars[1].getValue().getTrimmedText();
                // do something
            }
        }
    }
}
```

Looks awful, doesn't it? But there's a better way to do the same thing. You just need to extend a special interface --- [`DomElement`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/DomElement.java).

例如,让我们创建几个接口:


```java
interface Root extends com.intellij.util.xml.DomElement {
    Foo getFoo();
}

interface Foo extends com.intellij.util.xml.DomElement {
    List<Bar> getBars();
}

interface Bar extends com.intellij.util.xml.DomElement {
    String getValue();
}
```

接下来,您应该创建一个[`DomFileDescription`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/DomFileDescription.java)对象,将根标记名称和根元素传递给它的构造函数
接口,并使用扩展名`com.intellij.dom.fileDescription`注册它.

如果您的插件目标是2019.1或更高版本,请使用扩展点`com.intellij.dom.fileMetaData`,并在`plugin.xml`中指定`rootTagName`和`domVersion` /`stubVersion`.


您现在可以从[`DomManager`]获取文件元素(upsource:///xml/dom-openapi/src/com/intellij/util/xml/DomManager.java).
要获取“239”值,您只需编写以下代码:


```java
DomManager manager = DomManager.getDomManager(project);
Root root = manager.getFileElement(file).getRootElement();
List<Bar> bars = root.getFoo().getBars();
if (bars.size() > 1) {
    String s = bars.get(1).getValue();
    // do something
}
```

我想这看起来好一点.
您经常在多个地方使用您的模型.
重新创建模型的效率太低,因此我们为您缓存它,对“DomManager.getFileElement()”的任何后续调用都将返回相同的实例.
因此,仅调用此方法一次是有用的,然后只保留您获得的“根”对象.
在这种情况下,你不需要重复那个可怕的第一行,代码看起来会更好.


同样重要的是要注意,在这种情况下,我们避免了潜在的`NullPointerException`:我们的DOM保证访问tags子元素的每个方法都将返回一个not-null元素,即使相应命名的子标记不存在.
乍一看这看起来很奇怪,但看起来相当方便.
它是如何工作的？
简单.
给定这些接口,DOM生成用于访问正确子标签和在运行时创建模型元素的所有代码.
子标记名称和元素类型取自方法名称,返回类型和方法注释(如果有).
在大多数情况下,注释可以省略,如我们的示例中所示,但无论如何这将在本文中进一步讨论.


现在让我们更深入地探索DOM可以做什么,并研究表示各种XML概念的可能方式,例如标记内容,属性或子标记.
稍后我们将讨论使用该模型的基本方法,以及更多高级功能.
最后,我们将看到如何轻松地为DOM模型元素创建UI编辑器.


##构建模型


###标签内容


在XML PSI中,标记内容被称为标记值,​​因此一致性也是如此.
要读取和更改标记值,必须在界面中添加两个方法(getter和setter),如下所示:


```java
String getValue();
void setValue(String s);
```

这些方法名称(`getValue`和`setValue`)是标准的,默认情况下它们用于访问标记值.
如果要为同一目标使用自定义方法名称,则应使用`@ TagValue`注释这些方法,例如:


```java
@TagValue
String getTagValue();

@TagValue
void setTagValue(String s);
```

如您所见,我们的访问器使用`String`值.
这很自然,因为XML表示文本格式,标记内容始终是文本.
但有时您可能希望使用整数,布尔值,枚举甚至类名(它们当然将表示为`PsiClass`)和更通用的Java类型(`PsiType`).
在这种情况下,您只需将方法中的类型更改为您需要的类型,一切都将继续正常工作.


####自定义值类型


如果你使用更奇特的类型,你应该告诉DOM如何处理它们.
首先,使用`@ Convert`注释注释你的访问器方法,并指定你自己的类,它应该在注释中扩展`Converter <T>`类.
这里`T`是你的异国情调类型,而`Converter <T>`是一个知道如何在`String`和`T`之间转换值的东西.
如果无法转换该值(例如,“foo”不能转换为“Integer”),则转换器可能会返回“null”.
还请注意,您的实现应该有一个无参数的构造函数.


让我们考虑一个有趣的情况,`T`表示枚举值.
通常,转换器只搜索具有XML指定名称的枚举元素.
但有时,对于他们的名称,您可能需要或想要使用不是有效Java标识符的值.
例如,EJB中的CMP版本可能是“1.x”或“2.x”,但您无法使用此类名称创建Java枚举.
对于这种情况,让你的枚举实现`NamedEnum`接口,然后根据需要命名你的枚举元素.
现在,只需提供`getValue()`实现,它将返回正确的值以匹配XML内容,并且voilà\!

在我们的示例中,代码如下所示:


```java
enum CmpVersion implements NamedEnum {
    CmpVersion_1_X ("1.x"),
    CmpVersion_2_X ("2.x");

    private final String value;
    
    private CmpVersion(String value) { 
        this.value = value; 
    }

    public String getValue() { return value; }
}
```

正如我们已经提到的,XML标签除了它的值之外还可能有很多工件:可以有属性,子项,但通常(例如,根据DTD或Schema)它应该只有值.
当然,这样的标签也需要DOM元素来关联.
我们提供了这样一个元素:


```java
interface GenericDomValue<T> {
    T getValue();
    void setValue(T t);

    @TagValue
    String getStringValue();

    @TagValue
    void setStringValue(String s);
}
```

So, you can just specify a particular `T` when using this interface --- and everything will work. Methods that work with `String` are provided for many reasons. For example, your `T` is [`PsiClass`](upsource:///java/java-psi-api/src/com/intellij/psi/PsiClass.java). It would be useful to highlight invalid values in UI. To get the value to highlight (the string from the XML file) we have the `getStringValue()` method. The error message will be taken from the converter via `getErrorMessage()`.

###属性


属性也很容易处理.
您可以阅读它们的值,设置它们,并使用不同的类型进行操作.
所以很自然地创建类似`GenericDomValue <T>`的东西,然后像往常一样工作. 
“类似的东西”将成为继承者,如下所示:


```java
interface GenericAttributeValue<T> extends GenericDomValue<T> {
    XmlAttribute getXmlAttribute();
}
```

考虑您要使用名为_some-class_的属性,其值为`PsiClass`:


```java
@Attribute("some-class")
GenericAttributeValue<PsiClass> getMyAttributeValue();
```

就这样\!
现在你可以获取/设置值,解析这个`PsiClass`,得到它的`String`表示等.属性的名称将取自方法名称(参见下一段).
如果以特殊方式命名方法,甚至可以省略注释.
例如:


```java
GenericAttributeValue<PsiClass> getSomeClass();
```

[`DomNameStrategy`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/DomNameStrategy.java)接口指定如何将访问者名称转换为XML元素名称.
或者更准确地说,不是完整的访问者名称,而是名称减去任何“get”,“set”或“is”前缀.
策略类在任何DOM元素接口的`@ NameStrategy`注释中指定.
然后,此接口的任何后代和子级都将使用此策略.
默认策略是[`HyphenNameStrategy`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/HyphenNameStrategy.java),其中单词由连字符分隔(参见上面的示例).
另一个常见变体是[`JavaNameStrategy`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/JavaNameStrategy.java),它将每个单词的第一个字母大写,如Java的命名约定.
在我们的示例中,属性名称将为“someClass”.


如果属性没有定义`PsiClass`,而是一些需要转换器的自定义`T`,你只需要为getter指定`@ Convert`注释.


请注意,即使未在XML中指定属性,属性getter方法也永远不会返回`null`.
它的`getValue()`,`getStringValue()`和`getXmlAttribute()`方法将返回`null`,但DOM接口实例将存在且有效.
如果元素具有底层属性,则可以很容易地修复(当然,只有在需要时):只需调用`undefine()`方法(在`DomElement`中定义),并且XML属性消失,而[`GenericAttributeValue 
`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/GenericAttributeValue.java)仍然有效.


###儿童:固定号码


您可能经常在定义实体EJB的标签中处理具有至少一个具有给定名称的子标签的标签(例如,`<ejb-name>`,`<ejb-class>`或`<cmp-field>`).
与这些孩子一起工作,为他们提供吸气剂.
这些getter应该有一个扩展名为“DomElement”的返回类型:


```java
GenericDomValue<String> getEjbName();
GenericDomValue<String> getEjbClass();
CmpField getCmpField();
```

还有一个注释来明确指定这些孩子:`@ SubTag`.
其“value”属性包含标记名称.
如果未指定,则使用当前名称策略从方法名称隐含名称.


Sometimes it is the sub-tag's presence that means something, rather than its content --- `<unchecked>` in EJB method permissions, for example. If it exists, then permissions are unchecked, otherwise checked. For such things one should create a special `GenericDomValue<Boolean>` child. Usually its `getValue()` returns `true` if there's "true" in a tag value, `false` if there's "false" in a tag value, and `null` otherwise. In the `@SubTag` annotation, you can specify the attribute like `indicator=true`. In this case, `getValue()` will return `true` if the tag exists and `false` otherwise.

让我们考虑一个受EJB启发的另一个有趣的例子,其中有一个关系有两个角色,每个角色指定一个关系结束:第一个角色和第二个角色.
两者都由具有相同值的标签表示.
因此,我们可以创建角色元素的集合,每次我们访问某个角色时,我们都会检查此集合是否具有足够数量的元素.
但DOM的主要目的之一是消除不必要的检查.
那么为什么我们不能有一个固定的(多个)具有相同标签名称的孩子？
让我们拥有它们!


```java
@SubTag(value = "ejb-relationship-role", index = 0)
EjbRelationshipRole getEjbRelationshipRole1();

@SubTag(value = "ejb-relationship-role", index = 1)
EjbRelationshipRole getEjbRelationshipRole2();
```

The first method will return the DOM element for the first subtag named `<ejb-relationship-role>`, and the second --- for the second one. Hence the term "fixed-number" for such children. According to DTD or Schema, there should be fixed number of subtags with the given name. Most often this fixed number is 1; in our case with the relations it is 2. Just like attributes, fixed-number children exist regardless of underlying tag existence. If you need to delete tags, it can be done with the help of the same `undefine()` method.

对于[`GenericDomValue`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/GenericDomValue.java)类型的子类,您也可以像属性一样指定转换器.


###儿童:收藏


DTD和Schemas中的一个更常见的情况是,子项具有相同的标记名称和非固定的上限.
它们的访问器与下面的固定数字子的访问器不同:返回结果是扩展“DomElement”的特殊类型的“Collection”或“List”,如果要使用名称策略,则方法名称必须为
以复数形式.
例如,在EJB中我们将使用以下方法:


```java
List<Entity> getEntities();
```
There's also an annotation `@SubTagList` where you can explicitly specify the tag name.

无法直接修改返回的集合.
要从集合中删除元素,只需在此元素上调用`undefine()`.
然后将删除标记,元素将变为无效(`DomElement.isValid()== false`).
请注意,此行为与固定数字子项和属性的行为不同:它们始终有效,即使在`undefine()`之后也是如此.
同样,与那些子类型不同,集合子元素总是具有有效的基础XML标记.


添加元素有点困难.
由于所有DOM元素都是在内部创建的,因此您不能将一些DOM元素传递给某个方法,以将元素添加到集合中.
实际上,您必须要求父元素将子元素添加到集合中.
在我们的示例中,它按以下方式完成:


```java
Entity addEntity(int index);
```

它可以在任何地方添加元素,或者


```java
Entity addEntity();
```
which adds a new DOM element to the end of the collection. Please note the singular tense of the word "Entity". That's because here we deal with one `Entity` object, while in the collection getter we dealt with potentially many entities.

现在,您可以使用返回值执行任何操作:修改,定义标记的值,子项等.


最后一个常见的情况也是一个集合,但是由一个具有不同名称的标签组成的集合可以任意混合.
要使用它,您应该为混合集合中的所有标记名称定义集合getter,然后定义另一个特别注释的getter:


```java// <foo> elements
List<Foo> getFoos();

// <bar> elements
List<Bar> getBars();

// all <foo> and <bar> elements
@SubTagsList({"foo", "bar"})
List<FooBar> getMergedListOfFoosAndBars();
```
The annotation here is mandatory - we cannot guess several tag names from one method name.

要向此类混合集合添加元素,您应该为每个可能的标记名称创建“添加”方法:


```java
@SubTagsList(value={"foo","bar"}, tagName="foo") Fubar addFoo();
@SubTagsList(value={"foo","bar"}, tagName="bar") Fubar addBar(int index);
```
The index parameter in the last example means the index in the merged collection, not in the collection of tags named "bar".

###动态定义

您可以通过实现`com.intellij.util.xml.reflect.DomExtender <T>`在运行时扩展现有的DOM模型.
在EP`com.intellij.dom.extender`的“extenderClass”属性中注册它,其中“domClass”指定要扩展的DOM类`<T>`. 
[`DomExtensionsRegistrar`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/reflect/DomExtensionsRegistrar.java)提供了各种注册动态属性和子项的方法.


_13.1_

如果贡献的元素依赖于纯XML文件内容以外的任何内容(使用框架版本,类路径中的库,...),请确保从`DomExtender#supportsStubs`返回`false`.


###从现有XSD生成DOM

DOM可以从现有的XSD/DTD自动生成.
输出正确性/完整性在很大程度上取决于输入方案,可能需要额外的手动调整.


请遵循以下步骤(12.1或更高版本):


*在启用“插件DevKit”的情况下运行IntelliJ IDEA并添加JVM选项`-Didea.is.internal = true`

*选择工具 -->内部操作 --> DevKit  -->生成DOM模型

*选择Scheme文件并设置选项,然后单击“生成”以生成源

*根据您的需要修改生成的来源


### IDE支持

_IntelliJ IDEA 13_


* [`DomElement`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/DomElement.java):为继承类中定义的所有与DOM相关的方法提供隐式用法(以抑制“
未使用的方法“警告”

* [`DomElementVisitor`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/DomElementVisitor.java):为继承类中定义的所有与DOM相关的访问者方法提供隐式用法(以抑制
“未使用的方法”警告)


##使用DOM


###班选择者

通常会发生一个集合包含同名的标记,这些标记可能具有不同的结构,甚至可能由DTD或Schema中的不同类型表示.
例如,JSF Managed Beans可能有三种类型.
如果`<managed-bean>`标记包含`<map-entries>`子标记,那么Managed Bean类型是`MapEntriesBean`.
如果它包含一个`<list-entries>`子标签 - 你能猜到吗？
对 - “ListEntriesBean”!
否则它是一个`PropertyBean`(所有三个接口都扩展了`ManagedBean`).
当我们编写`List <ManagedBean> getManagedBeans()`时,我们期望不仅得到一个列表,其中所有元素都是`ManagedBean`接口的实例,而是一个列表,其中每个元素都是某种类型,即`MapEntriesBean` 
,`ListEntriesBean`或`PropertyBean`.


在这种情况下,应该决定DOM元素应该实际实现哪个接口(根据给定的标记).
这是通过扩展[`TypeChooser`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/TypeChooser.java)抽象类来实现的:


```java
public abstract class TypeChooser {
    public abstract Type chooseType(XmlTag tag);
    public abstract void distinguishTag(XmlTag tag, Type aClass) throws IncorrectOperationException;
    public abstract Type[] getChooserTypes();
}
```

这里,第一个方法(`chooseType()`)完全按照它的名字命名(选择特定类型,通常是类).
第二个(`distinguishedTag()`)反向运行:它修改标签,以便下次从XML文件读取元素时(例如,在用户关闭并再次打开项目之后),新创建的
DOM元素将实现相同的接口,不会丢失任何模型数据.
最后,`getChooserTypes()`只返回`chooseType()`可以返回的所有类型.


要使`TypeChooser`工作,请通过调用`registerTypeChooser()`在重写的`DomFileDescription.initializeFileDescription()`方法中注册它.


###有用的DomElement和DomManager方法


#### PSI连接

当然,DOM与XML PSI紧密相连,因此总有一种方法可以使用`getXmlTag()`方法获取`XmlTag`实例(对于固定数字的子节点和属性可以是`null`).
我们记得在`GenericAttributeValue`中还有`getXmlAttribute()`方法.
一般情况下有`getXmlElement()`方法.
您还可以使用`DomManager.getDomElement()`方法通过其基础XML PSI元素获取DOM元素.


如果DOM元素没有底层XML元素,可以通过调用`ensureTagExists()`来创建它.
要删除标记,请使用已知的`undefine()`方法.
此方法将始终删除基础XML元素(标记或属性).
如果元素是一个集合子元素,那么它和它的整个子树都不再有效.


####树结构

在每棵普通的树上,总有可能走路. 
`DomElement`也不例外.
方法`getParent()`只返回树中元素的父级.


方法`<T extends DomElement> T getParentOfType(Class <T> requiredClass,boolean strict)`返回给定类的树祖先.
您可以看到标准_strict_参数,它可以返回DOM元素本身,如果它是`false`并且您当前的DOM元素是_requiredClass_的实例.


最后,`getRoot()`将返回`DomFileElement`,它是每个DOM树的根.


####有效性

如果明确删除元素或由于外部PSI更改,则该元素将变为无效.
固定数量的子项和属性旨在尽可能长时间保持有效,无论XML发生什么.
只有当它们具有已删除的集合树祖先时,它们才会变为无效.


新创建的DOM元素总是正确有效的,因此它们的`isValid()`方法将返回`true`.


元素有效性非常重要,因为你无法在无效元素上调用任何方法(当然,除了`isValid()`本身).


#### DOM反射

DOM也有一种反射,称为“通用信息”.
可以使用它来直接访问标记名称,而不是调用getter方法.
参见``DomGenericInfo`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/reflect/DomGenericInfo.java)接口和``DomElement`和`DomManager`中的`getGenericInfo()`方法
欲获得更多信息.
还有`DomElement.getXmlElementName()`方法,它返回相应标记或属性的名称.


#### 介绍

<!--  TODO:使用@Presentation  -->


`DomElement.getPresentation()`返回[`ElementPresentation`]的实例(upsource:///xml/dom-openapi/src/com/intellij/util/xml/ElementPresentation.java),这是一个知道可呈现元素类型的接口
,名称,有时甚至是它的图标.
演示实际上是从演示工厂对象获得的,像ClassChoosers一样,应该尽早在[`ElementPresentationManager`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/ElementPresentationManager.java)中注册
尽可能.
您可以为某些类的所有元素指定类型名称和图标,获取特定对象的类型名称,图标和可显示名称的方法.
如果未指定,则可以从对象本身获取可显示的名称,如果它包含使用`@ NameValue`注释注释的方法,则返回`String`或`GenericValue`.
如果没有这样的方法,它将返回“null”.
对于`DomElement`,还有另一种获得这个可呈现名称的方法:

`DomElement.getGenericInfo().getElementName()`.


####活动

如果您希望收到有关DOM模型中每个更改的通知,请将`DomEventListener`添加到`DomManager`. 
DOM支持以下事件:标记值已更改,元素已定义/未定义/已更改,以及已添加/删除的集合子项.


####突出显示注释

DOM支持错误检查和突出显示.
它基于您在特殊位置添加到DOM元素的注释(不要将这些注释与Java 5的注释混淆 - 它们非常不同).
你需要实现[`DomElementAnnotator`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/highlighting/DomElementsAnnotator.java)接口,并覆盖`DomFileDescription.createAnnotator()`方法
,并在那里创建这个注释器.
在`DomElementsAnnotator.annotate(DomElement element,DomElementsProblemsHolder annotator)中,您应该将元素子树中的所有错误和警告报告给注释器(`DomElementsProblemsHolder.createProblem()`).
您应该在`DomFileDescription`的相应虚拟方法中返回此注释器.


####自动突出显示(BasicDomElementsInspection)

通过提供`BasicDomElementsInspection`的实例,可以自动突出显示以下错误:


 - 缺少`@ Required`元素或具有空文本

 - 某些`Converter`无法转换XML值

 - 名称不应该是唯一的


后一种情况要求您使用`@ NameValue`注释指定名称getter.
检查使用`DomFileDescription.getIdentityScope()`方法来获取定义名称应该唯一的根作用域的元素.


要禁止拼写检查,请使用`@ com.intellij.spellchecker.xml.NoSpellchecking`注释您的DomElement.


####需要的孩子

当需要说一些必需的子标签或属性丢失时,错误突出显示有一个常见的情况.
如果使用`@ Required`注释为该子项注释getter,DOM将自动为您执行此操作.
对于收集子getter,这个注释意味着该集合不应该为空(对应于DTD中的'+'符号).
此外,当您创建一个需要固定数字或属性子元素的新元素时,它们的标记或属性也将以XML格式创建.




###解决

还记得接口`GenericDomValue <T>`及其子接口`GenericAttributeValue <T>`？
请记住,任何类都可以作为`T`传递 - 例如,让我们将`GenericDomValue <PsiClass>`解释为对类的引用.
然后我们总是可以将它视为对`T`类对象的引用!
使用字符串或枚举,它不是一个非常有用的想法,但我们将以另一种方式使用它.
通常,XML具有这样的结构:在某个地方声明对象,并在其他地方(更确切地说,在标记或属性值中)引用.
因此,如果你想创建一个类似`GenericValue <MyDomElement> getMyDomElementReference()`的方法,那么你只需要指定一个适当的转换器,它将在你的`MyDomElement`模型中找到一个实例,其名称在`GenericDomValue中指定. 
.


这是核心理念.
由于创建这样的转换器非常无聊,我们已经为您完成了.
您根本不需要注释引用getter,因为名称解析将自动进行.
将按名称搜索元素,名称将从使用`@ NameValue`注释的方法中获取.
使用的转换器是[`DomResolveConverter`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/DomResolveConverter.java).
它的构造函数接受一个参数,因此它不能在`@ Convert`注释中引用,但它的子类(如果你创建它们) - 可以.
如果您仍想明确指定您对“DomElement”的引用应该在“模型范围内”解析,请使用带有所需类参数化的`@ Resolve`注释.
解析范围将取自`DomFileDescription.getResolveScope()`.


除了上述内容之外,DOM中的自动解析还提供了XML文本编辑器中的一些功能:错误突出显示,完成,查找用法,重命名重构...未解析的引用将突出显示,甚至完成.
如果你想创建一个自定义转换器并希望对它有这个代码的洞察力,你不仅应该扩展[`Converter`](upsource:///xml/dom-openapi/src/com/intellij/util/xml /Converter.java)但是[`ResolvingConverter`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/ResolvingConverter.java).
它还有一个方法`getVariants()`,你必须提供由你的引用可以解析的所有目标组成的集合.
那些熟悉[`PsiReference`](upsource:///platform/core-api/src/com/intellij/psi/PsiReference.java)的人会认识到这里的相似之处.


如果你需要根据其他值(例如兄弟/父元素)或任何运行时条件(例如库的存在或版本)选择`Converter`,你可以使用`WrappingConverter`.
另请参阅`GenericDomValueConvertersRegistry`,以管理可供选择的转换器的可扩展注册表.


###模拟和稳定元素

您的DOM元素不必绑定到物理文件. 
`DomManager.createMockElement()`将帮助您使用给定模块创建给定类的虚拟元素.
元素可以是物理的或不是物理的.
这里的“物理”意味着DOM将为它创建一个模拟文档,因此如果您将此文档传递到文件编辑器中的正确位置,则可以享受撤消功能.


`DomElement.copyFrom()`允许您将信息从一个`DomElement`复制到另一个.
实际上,它只是替换了XML标记,并且丢失了所有旧数据.
然而,元素的固定数字子元素不会变为无效,它们只包含新标记值,属性值等.树实际上相当保守.


`createMockElement()`和`copyFrom()`的组合对于编辑对话框中的元素内容很有用.
您可以创建元素的模拟副本,在对话框中使用它,然后,如果用户未取消,则将元素复制回主模型.
由于这是一种常见的情况,因此在`DomElement`中创建了一个特殊的快捷方法,称为`createMockCopy()`.


IntelliJ平台的XML解析器是增量的:文本中的更改不会导致整个文件被重新分析.
但是您应该记住,此规则有时可能无法正常工作.
例如,由于手动编辑XML文件,您的DOM元素可能会意外地中断(即使它们不在这些元素中发生).
如果文件编辑器依赖于这样一个破碎的元素,这可能导致关闭选项卡,从用户的角度来看这不是很好.
例如,假设您有一个名为“SomeEntity”的实体bean.
您打开它的编辑器,然后进入XML,将标签名称从实体更改为会话,然后再更改为实体.
当然,在这种亵渎之后,没有DOM元素可以存活.
但尽管如此,您仍然希望您的编辑保持开放!
好吧,有一个解决方案,它叫做'DomManager.createStableValue(工厂工厂)`.

此方法创建一个DOM元素,将其所有功能委托给某个真实元素(从factory参数返回).
一旦该真实元素变为无效,工厂将再次被调用,如果它返回有效的内容,它将成为新的委托.
依此类推......在使用EJB的示例中,工厂将再次查找名为“SomeEntity”的实体Bean.


稳定的DOM元素还实现了`StableElement`接口,它具有以下方法:


*`DomElement getWrappedElement()` - 只返回委托所有方法调用的当前元素;

*`void invalidate()` - 使包装元素无效.
以下任何方法调用都将导致工厂创建新的委托;

*`void revalidate()` - 调用工厂,如果返回新内容(即与当前包装元素不同),则使旧值无效并采用新值.


###访客

访客是一种非常常见的设计模式. 
DOM模型也有一个访问者,它叫做'DomElementVisitor`. 
`DomElement`接口有方法`accept()`和`acceptChildren()`,它将此访问者作为参数.
如果你看一下接口`DomElementVisitor`本身,你可能会感到惊讶,因为它只有一个方法:`visitDomElement(DomElement)`.
访客模式在哪里？
那些通常在其中找到名称如“visitT(T)”的方法在哪里？
没有这样的方法,因为除了你以外的任何人都不知道实际的接口(T).
但是当你实例化`DomElementVisitor`接口时,你可以添加这些`visitT()`方法,然后调用它们!
你甚至可以将它们命名为`visit()`,指定参数的类型,一切都会好的.
例如,如果您有两个DOM元素类 - “Foo”和“Bar” - 您的访问者可能如下所示:


```java
class MyVisitor implements DomElementVisitor {
    void visitDomElement(DomElement element) {}
    void visitFoo(Foo foo) {}
    void visitBar(Bar bar) {}
}
```

###实施

有时您可能希望使用一些与XML没有直接关联但与程序逻辑相关的功能来扩展模型.
这个功能最合适的地方是DOM元素接口.
该怎么办？


最简单的情况是,当您想要向您的接口添加一个方法,该方法返回此元素(或其子代之一)中的其他getter返回的确切内容.
您可以轻松编写此辅助方法并使用`@ PropertyAccessor`批注对其进行批注,其中您应指定由属性名称组成的路径(不带“get”或“is”前缀的getter名称).
例如,你可以写:


```java
GenericDomValue<String> getVeryLongName()

@PropertyAccessor("very-long-name")
GenericDomValue<String> getName()
```
In this case, the second method will return just the same as the first one. If there were "foo.bar.name" instead of "very-long-name" in the annotation, the system would actually call `getFoo().getBar().getName()` and return the result to you. Such annotations are useful when you're extending some interface that is inconsistent with your model, or you try to extract a common super-interface from two model interfaces with differently named children that have the same sense (see `<ejb-ref>` and `<ejb-local-ref>`).

刚才描述的案例很简单,但很少见.
更常见的是,您必须在模型中加入一些逻辑.
除了Java代码之外什么都没有帮助你.
它会.
将所需的方法添加到您的接口,然后创建一个实现该接口的抽象类,并在那里仅实现您手动添加并且不直接连接到XML模型的方法.
请注意,该类应该具有不带参数的构造函数.


现在,您只需要让DOM知道您希望每次创建应该实现必要接口的模型元素时都使用此实现.
只需使用注册即可

扩展点`com.intellij.dom.implementation`和DOM将在运行时生成不仅实现所需接口的类,还扩展抽象类.


###跨多个文件的模型

许多框架需要一组XML配置文件(“文件集”)作为一个模型工作,因此解析/导航适用于所有相关的DOM文件.

根据实现/插件,可以实现隐式提供文件集(使用项目中现有框架的设置)或通过用户配置(通常通过专用的“Facet”).


扩展[`DomModelFactory`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/model/impl/DomModelFactory.java)(或[`BaseDomModelFactory`](upsource:///xml /dom-openapi/src/com/intellij/util/xml/model/impl/BaseDomModelFactory.java)用于非`模块`范围)并提供你的`DomModel`的实现.
通常,您需要添加搜索器/实用程序方法来使用`DomModel`实现.

示例可以在Struts 2插件中找到(包`com.intellij.struts2.dom.struts.model`).


### DOM Stubs

_请谨慎使用它,仅用于DOM模型中访问量大的部分,因为它会增加磁盘空间使用/索引运行时间_


DOM元素可以存根,因此不需要(昂贵)访问XML/PSI(有关自定义语言的类似功能,请参阅TODO [Indexing和PSI Stubs]).
性能相关的元素,标记或属性getter可以简单地用`@ com.intellij.util.xml.Stubbed`注释.

每当您在DOM层次结构中更改`@Stubbed`注释用法时,都会从`DomFileDescription#hasStubs`返回`true`并增加`DomFileDescription#getStubVersion`,以在索引编制期间触发正确重建Stub.


##构建基于DOM的GUI


### 形式

处理DOM的所有表单都以特殊方式组织.
它们支持两个主要内容:从XML获取数据到UI,以及将UI数据保存到XML.
前者称为重置,后者称为重置.
有一个[`Committable`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/ui/Committable.java)接口,它有相应的方法:`commit()`和`reset() 
`.
还有一种方法可以将表单结构化为更小的部分,即Composite模式:[`CompositeCommittable`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/ui/CompositeCommittable.java) 
.
方法`commit()`和`reset()`在编辑器选项卡切换或撤消时自动调用.
所以你只需要确保你所有的Swing结构都在一个`CompositeCommittable`树中组织起来,所有的辛苦工作都将由IDE来完成.


DOM控件是`Committable`的特殊后代.
所有这些都实现了`DomUIControl`.
请注意,它们不是Swing组件 - 它们只是连接DOM模型和Swing组件的一种方式.
连接的一端 -  DOM元素 - 通常在控件构造函数中指定.
另一端 -  Swing组件 - 可以通过两种方式获得.
首先是要求DOM控件创建它.
但是,如果您想在IntelliJ IDEA的GUI Designer中创建表单,那将非常不方便.
在这种情况下,您将需要第二种方式:向控件请求`bind()`到正确类型的现有Swing组件(这取决于您正在编辑的值的类型).
之后,您的Swing组件将与DOM同步,它们甚至会突出显示`DomElementsAnnotator`报告的错误.


有时您可能需要在提交特定DOM控件后执行一些工作(启用或禁用某些组件,更改其值).
然后你应该定义该DOM控件的`addCommitListener()`方法并覆盖`CommitListener.afterCommit()`方法.
此方法将在与主`commit()`相同的写操作内调用,因此您在此方法中对XML执行的任何更改都将与撤消队列中的`commit()`合并.


###简单控制

使用简单的控件,您可以编辑`GenericDomValue`:简单文本,类名,枚举和布尔值.
这些控件将特殊对象作为构造函数参数.
该对象应该实现`DomWrapper`接口,该接口知道如何设置/获取DOM模型的值.


我们有三个主要的DomWrapper:[`DomFixedWrapper <T>`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/ui/DomFixedWrapper.java)将调用重定向到[`GenericDomValue <T 
>`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/GenericDomValue.java),[`DomStringWrapper`](upsource:///xml/dom-openapi/src/com /intellij/util/xml/ui/DomStringWrapper.java)将调用重定向到[`GenericDomValue`]的字符串访问器(upsource:///xml/dom-openapi/src/com/intellij/util/xml/GenericDomValue.java)
和[`DomCollectionWrapper`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/ui/DomCollectionWrapper.java)获取/设置给定[`GenericDomValue的第一个元素的值
`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/GenericDomValue.java)集合.
一些控件(那些具有文本字段作为其自身的一部分)采用额外的布尔构造函数参数--_commitOnEveryChange_,其含义从名称中显而易见.
除了小对话框之外,我们不建议在任何地方使用它,因为提交每个更改会显着降低系统速度.


这些控件通常由`DomUIFactory.createControl(GenericDomValue)`创建.
这个方法通过使用DOM反射来理解要创建的控件(你可能还记得`DomGenericInfo`).
但有时您可能想直接创建控件.
让我们更仔细地看一下简单的控件.


##### BooleanControl

它允许您编辑布尔值.
该控件绑定到`JCheckBox`.


![BooleanControl](/xml_dom_api/booleancontrol.gif)


##### ComboControl

该控件绑定到一个不可编辑的`JComboBox`,因此它可用于从有限集中选择一些东西.
这种有限集合的一个案例是枚举.
或者它可以是一个构造函数,您可以在其中提供`Factory <List <String >>`,并从此工厂返回您想要的任何内容(例如,可供选择的数据库名称列表).
默认情况下,错误的值(以XML格式编写,但不会出现在您为控件提供的列表中)以红色显示.
由于通常的做法是为组合框指定自定义的“CellRenderer”,控件具有`isValidValue(String)`方法.
如果它对您正在渲染的值返回“false”,则可以以某种方式突出显示它,以获得与默认渲染器相同的结果.
或者您可以以自己的方式委托给该渲染器.


![ComboControl](/xml_dom_api/combocontrol.gif)


##### BooleanEnumControl

有时,当只有两个选项时,使用复选框而不是组合框很方便.
该控件专为此类情况而设计.
在被复选框(并被绑定)时,控件不仅编辑“true”或“false”,而且编辑任意两个String值或两个枚举元素.
在最后一种情况下,它有一个布尔_invertedOrder_参数,用于指定哪个元素对应于checked状态.
默认情况下,_invertedOrder_设置为“false”,因此第一个元素对应于未选中状态,第二个元素对应于已选中状态.
如果将参数设置为“true”,则状态将交换.


###基于编辑器的控件

请注意,基于编辑器的控件是基于IntelliJ Platform的`Editor`而不是标准的`JTextField`构建的.
由于目前无法通过Open API直接实例化Editor,因此控件绑定到特殊的`JPanel`继承器,并且它们的`bind()`方法将必要的内容添加到这些面板.


##### TextControl

此控件允许您编辑简单的字符串值.
该控件绑定到`TextPanel`组件.
还有该面板的继承者 - “MultiLineTextPanel”.
如果将`StringControl`绑定到它,屏幕上会出现一个大编辑器.
如果您没有大型编辑器的空间,请将其绑定到“BigTextPanel”.
然后它将填充文本编辑器,并添加浏览按钮以打开一个大编辑器的对话框,您可以在其中键入更长的字符串.


##### PsiClassControl

这是一个带有浏览按钮的单行编辑器,可以打开标准的类选择对话框.
该控件仅接受类名.
它被绑定到'PsiClassPanel`.


![PsiClassControl](/xml_dom_api/psiclasscontrol.gif)


##### PsiTypeControl

这与PsiClassControl几乎相同,但不仅允许输入类名,还允许输入Java基元类型甚至数组.
它被绑定到'PsiTypePanel`.


###收集控制

有一个特殊的表组件,其中每一行代表一个集合子代.
它被称为`DomCollectionControl <T>`,其中`T`是你的集合元素类型.
要正常运行,它需要`DomElement`(集合的父级),集合的一些描述(子标记名称或来自DOM反射的`DomCollectionChildDescription`)和`ColumnInfo`数组.
这可以传递给构造函数,也可以在`DomCollectionControl`继承器中,在重写方法`createColumnInfos()`中创建.


什么是专栏信息？
使用表模型只是一种更舒适的方式.
它使用Java 5泛型并且更加面向对象.
因此,它被命名为`ColumnInfo <Item,Aspect>`,其中`Item`是对应于集合中元素类型的类型变量,而`Aspect`是对应于此特定列信息类型的类型变量:`String` 
,'PsiClass`,`Boolean`等.列知道的基本内容是:列名,列类,读取值(Aspect`valueOf(Item)`),写入值(`setValue(Item item,Aspect aspect) 
`),单元格渲染器(`getRenderer(Item)`),单元格“可编辑性”(`isCellEditable(Item)`),单元格编辑器(`getEditor(Item)`)等.


有很多预定义的列信息,所以你可能永远不会创建一个新的.


首先,如果集合子元素是`GenericDomValue`,通常可以直接在表中编辑它.
为此,您可能需要以下类之一:`StringColumnInfo`,`BooleanColumnInfo`或更通用的`GenericValueColumnInfo`.
但很少遇到这样的集合.


更常见的情况是集合元素更复杂并且具有多个`GenericDomValue`子元素.
然后可以为每个孩子创建一个列.
适当的列信息是`ChildGenericValueColumnInfo <T>`.
它会要求你提供一个'DomFixedChildDescription`(来自DOM反射的另一件事),一个渲染器和一个编辑器 - 没有别的.
因此,自定义的主要内容是渲染器和编辑器.


至于渲染器,有两个主要选择:`DefaultTableCellRenderer`和IntelliJ Platform的`BooleanTableCellRenderer`.
编辑器更复杂,但它们非常类似于简单的DOM控件.


`BooleanTableCellEditor`,`DefaultCellEditor(JTextField)`,`ComboTableCellEditor`等``DomUIFactory.createCellEditor()`会自动创建它们中的任何一个(包括`PsiClass`的编辑器),这样你就不需要考虑
每次选择哪一个.


集合控件是一个复杂的控件,因此它绑定到一个复杂的Swing组件.
它被称为[`DomTableView`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/ui/DomTableView.java).
它有一个工具栏(您可以使用添加和删除按钮覆盖`DomTableView.getToolbarPosition()`以自定义其位置).
如果需要,可以在`DomCollectionControl.createAdditionActions()`中指定自定义添加操作(建议扩展`ControlAddAction`).
如果只有一个添加动作,则在按下“添加”按钮后将调用它;
如果有很多,则会显示一个弹出菜单.
要更改删除策略,请覆盖`DomCollectionControl.doRemove(List <T>)`方法.


如果指定`DomCollectionControl.isEditable()`,工具栏也可能有一个Edit按钮.
要向此按钮添加行为,请覆盖“DomCollectionControl.doEdit(T)”.
如果在构造`DomTableView`时传递非null String _helpId_参数,也可以有一个帮助按钮.


如果集合中没有项目,`DomTableView`可能会显示一个特殊文本(`DomTableView.getEmptyPaneText()`),而不是空表.


您可以将自己的弹出菜单添加到控件中.
构造后调用`DomTableView.installPopup()`方法,并使用弹出操作传递`DefaultActionGroup`.


表可以有单个或多个(默认)行选择.
如果要更改此行为,请覆盖`DomTableView.allowMultipleRowsSelection()`.


![CollectionControl](/xml_dom_api/collectioncontrol.gif)


### UI组织

创建基于DOM的UI表单的最简单方法是扩展[`BasicDomElementComponent`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/ui/BasicDomElementComponent.java)类.
这将要求您将一些DOM元素传递给构造函数.
然后将IntelliJ IDEA GUI Designer表单绑定到子类,并在那里设计一个漂亮的表单.
您肯定希望将一些控件绑定到DOM UI,在这种情况下,您当然应该确保它们具有正确的类型.
最后,您应该在类的构造函数中创建一些DOM控件并绑定它们.
但是你可以自动创建控件并将它们绑定到`DomElement`的子节点 - “GenericDomValue”.


只需正确命名组件并在构造函数中调用`bindProperties()`方法.
字段名称应对应于元素子项的getter名称.
它们也可能以“我的”为前缀.
想象一下,你有这样的DOM接口:


```java
public interface Converter extends DomElement {
    GenericDomValue<String> getConverterId();
    GenericDomValue<PsiClass> getConverterClass();
}
```

在这种情况下,UI表单类可以如下所示:


```java
public class ConverterComponent extends BasicDomElementComponent<Converter> {
    private JPanel myRootPane;
    private TextPanel myConverterId;
    private PsiClassPanel myConverterForClass;

    public ConverterComponent(final Converter domElement) {
        super(domElement);

        bindProperties();
    }
}
```
All the fields here are actually bound to controls in a GUI form.

通常,您必须创建自己的文件编辑器.
然后,为了使用所有绑定和撤消功能,建议从[`继承你的[`FileEditorProvider`](upsource:///platform/platform-api/src/com/intellij/openapi/fileEditor/FileEditorProvider.java). 
PerspectiveFileEditorProvider`](upsource:///xml/dom-openapi/src/com/intellij/util/xml/ui/PerspectiveFileEditorProvider.java),创建[`DomFileEditor`]的实例(upsource:///xml/dom 
-openapi/src/com/intellij/util/xml/ui/DomFileEditor.java)然后传递一个[`BasicDomElementComponent`](upsource:///xml/dom-openapi/src/com/intellij/util/xml)/ui/BasicDomElementComponent.java).
要轻松创建一个顶部带有标题的编辑器,就像我们的EJB和JSF一样,您可以使用静态方法`DomFileEditor.createDomFileEditor()`. 
`DomFileEditor`自动监听与给定DOM元素对应的文档中的所有更改,因此在撤消时刷新组件.
如果要监听其他文档中的更改,请在`DomFileEditor`中使用`addWatchedDocument()`,`removeWatchedDocument()`,`addWatchedElement()`,`removeWatchedElement()`方法.


##结论

感谢您的时间和关注.
我们希望您发现这篇文章非常有用.
欢迎您将您的问题和意见发布到我们的[开放API和插件开发论坛][Open API and Plugin Development Forum](https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development).


###更多材料

以下捆绑的开源插件使(重)使用DOM:


 -  [Android](http://git.jetbrains.org/?p=idea/android.git;a=tree;f=android;;hb=HEAD)

 -  [Ant](upsource:///plugins/ant)

 -  [Plugin DevKit](upsource:///plugins/devkit)

 -  [Maven](upsource:///plugins/maven)

 -  [Struts 2](https://github.com/JetBrains/intellij-plugins/tree/master/struts2)(终极版)


