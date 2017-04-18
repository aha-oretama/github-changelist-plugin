# github-changelist-plugin

This Jenkins plugin gets updated files from GitHub.

This is useful plugin for a following case.

If you have too many and too slow test cases
and you want to restrict test cases affected by updated files,
you can get the updated file's paths and extract part of path you need.
Then you can restrict to execute test cases by using updated files and test-tools like Maven.

# Restrictions

* Support using in [Pipeline](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Groovy+Plugin), not support free-style job
* Now, support only using with [Pipeline Multibranch Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Multibranch+Plugin).
* Support only GitHub, GitHub Enterprise

# Required Jenkins Plugins

* Credentials Plugin (https://wiki.jenkins-ci.org/display/JENKINS/Credentials+Plugin)
* GitHub API Plugin (https://wiki.jenkins-ci.org/display/JENKINS/GitHub+API+Plugin)
* GitHub Branch Source Plugin (https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Branch+Source+Plugin)
* Pipeline Multibranch Plugin (https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Multibranch+Plugin)
* Pipeline Step API Plugin (https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Step+API+Plugin)

# How to use
```groovy
#!groovy
pipeline {
  agent any
  stages {
    stage('get changelist') {
      steps {
        script {
           // if updated files are 
           // 'src/main/java/aha/oretama/jp/GitHubChangelistStep.java', 
           // 'src/main/java/aha/oretama/jp/RegexUtils.java',
           // 'src/test/java/aha/oretama/jp/RegexUtilsTest.java'
          
           def changes = changelist regex: '([^/]*$)', testTargetRegex: '$1' 
           echo changes.join(',')
           // display 'GitHubChangelistStep.java,RegexUtils.java,RegexUtilsTest.java'
          
           def defaultChanges =  changelist() // default regex: '([^/]*?)(Test)?(\..*)?$', testTargetRegex = '**/$1Test*'
           echo defaultChanges.join(',')
           // display '**/GitHubChangelistStepTest*,**/RegexUtilsTest*'
        }
      }
    }
  }
}
```

## Features
* `regex` must have at least one group
* `testTargetRegex` is made from group index and optional added word,  
such as `$1`, `$1Test$3`
* `testTargetRegex` is replaced by groups matched in `regex`  
* default `regex` is `([^/]*?)(Test)?(\..*)?$`
* default `testTargetRegex` is `**/$1Test*`

# Best Practice

In restricting test cases affected by updated files,
you can implement by using this plugin and testing tools like Maven.
Here is the example of using Maven.


You add `inclusions.txt` written as follows.
```text
**/Test*.java
**/*Test.java
**/*Tests.java
**/*TestCase.java
```

These are default values in [includes](http://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#includes) parameter in [Maven Surefire Plugin](http://maven.apache.org/surefire/maven-surefire-plugin/).

Next, you add `includes`, `includesFile` in pom.xml.

```xml
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.19.1</version>
        <configuration>
            <argLine>-Dfile.encoding=UTF-8</argLine>
            <includes>
                <include>Nothing</include>
            </includes>
            <includesFile>${project.basedir}/inclusions.txt</includesFile>
        </configuration>
    </plugin>
```


These you've done are to be able to change executing test only by `inclusions.txt`.


Therefore, you write pipeline as follows, you can restrict test cases affected by updated files.
```groovy
pipeline {
  agent any
  stages {
    stage('Execute test case affected by changed files') {
      steps {
        script {
          def changes = changelist()
          echo changes.join(',')
          if(!changes) {
            // No changes, do not run test.
            return
          }
    
          // Execute only test cases affected by changed files
          writeFile file: 'inclusions.txt', text: changes.join('\n')
          sh 'mvn clean test'
    
          // Note that no surefire-repot if no test run.
          def exists = fileExists 'target/surefire-reports'
          if(exists) {
            junit 'target/surefire-reports/**/*.xml'
          }
        }
      }
    }
  }
}
```
