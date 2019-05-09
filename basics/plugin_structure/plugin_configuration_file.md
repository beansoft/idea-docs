---
title: Plugin Configuration File - plugin.xml
---

以下是示例插件配置文件.
此示例展示并描述了可以在`plugin.xml`文件中使用的所有元素.


```xml
<!-- `url` specifies the URL of the plugin homepage (can be opened from "Plugins" settings dialog) -->
<idea-plugin url="https://www.jetbrains.com/idea">

  <!-- Plugin name -->
  <name>VssIntegration</name>

  <!-- Unique identifier of the plugin.
       Cannot be changed between the plugin versions.
       If not specified, assumed to be equal to <name>. -->
  <id>com.jetbrains.vssintegration</id>

  <!-- Description of the plugin. -->
  <description>Vss integration plugin</description>

  <!-- Description of changes in the latest version of the plugin.
       Displayed in the "Plugins" settings dialog and in the
       plugin repository Web interface. -->
  <change-notes>Initial release of the plugin.</change-notes>

  <!-- Plugin version -->
  <version>1.0</version>

  <!-- The vendor of the plugin.
       The optional "url" attribute specifies the URL of the vendor homepage.
       The optional "email" attribute specifies the e-mail address of the vendor.-->
  <vendor url="https://www.jetbrains.com" email="support@jetbrains.com" />

  <!-- The unique identifiers of the plugins on which this plugin depends. -->
  <depends>com.MyFirstPlugin</depends>

  <!-- Optional dependency on another plugin.
       If the plugin with the "com.MySecondPlugin" ID is installed,
       the contents of mysecondplugin.xml (the format of this file
       conforms to the format of plugin.xml) will be loaded. -->
  <depends optional="true" config-file="mysecondplugin.xml">com.MySecondPlugin</depends>

  <!-- Allows a plugin to integrate its help system (in JavaHelp format)
       with the IDE help system. The "file" attribute specifies the name
       of the JAR file in the "help" subdirectory of the plugin directory.
       The "path" attribute specifies the name of the helpset file within
       the JAR file.-->
  <helpset file="myhelp.jar" path="/Help.hs" />

  <!-- Minimum and maximum build of IDE compatible with the plugin -->
  <idea-version since-build="183" until-build="183.*"/>

  <!-- Resource bundle (/messages/MyPluginBundle.properties) to be used
       with `key` attributes in extension points and implicit keys like
       `action.[pluginID].[ActionID].text` -->
  <resource-bundle>messages.MyPluginBundle</resource-bundle>

  <!-- Plugin's application components -->
  <application-components>
    <component>
      <!-- Component's interface class -->
      <interface-class>com.foo.Component1Interface</interface-class>

      <!-- Component's implementation class -->
      <implementation-class>com.foo.impl.Component1Impl</implementation-class>
    </component>
  </application-components>

  <!-- Plugin's project components -->
  <project-components>
    <component>
      <!-- Interface and implementation classes are the same -->
      <implementation-class>com.foo.Component2</implementation-class>

      <!-- If the "workspace" option is set "true", the component
           saves its state to the .iws file instead of the .ipr file.
           Note that the <option> element is used only if the component
           implements the JDOMExternalizable interface. Otherwise, the
           use of the <option> element takes no effect.  -->
      <option name="workspace" value="true" />

      <!-- If the "loadForDefaultProject" tag is present, the project component is instantiated also for the default project. -->
      <loadForDefaultProject/>
    </component>
  </project-components>

  <!-- Plugin's module components -->
  <module-components>
    <component>
      <implementation-class>com.foo.Component3</implementation-class>
    </component>
  </module-components>

  <!-- Actions -->
  <actions>
    <action id="VssIntegration.GarbageCollection" class="com.foo.impl.CollectGarbage" text="Collect _Garbage" description="Run garbage collector">
      <keyboard-shortcut first-keystroke="control alt G" second-keystroke="C" keymap="$default"/>
    </action>
  </actions>

  <!-- Extension points defined by the plugin.
       Extension points are registered by a plugin so that other
       plugins can provide this plugin with certain data. The
       "beanClass" attribute specifies the class the implementations
       of which can be used for the extension point. -->
  <extensionPoints>
    <extensionPoint name="testExtensionPoint" beanClass="com.foo.impl.MyExtensionBean"/>
  </extensionPoints>

  <!-- Extensions which the plugin adds to extension points
       defined by the IntelliJ Platform or by other plugins.
       The "defaultExtensionNs " attribute must be set to the
       ID of the plugin defining the extension point, or to 
       "com.intellij" if the extension point is defined by the
       IntelliJ Platform. The name of the tag within the <extensions>
       tag matches the name of the extension point, and the
       "implementation" class specifies the name of the class
       added to the extension point. -->
  <extensions xmlns="VssIntegration">
    <testExtensionPoint implementation="com.foo.impl.MyExtensionImpl"/>
  </extensions>
</idea-plugin>
```


