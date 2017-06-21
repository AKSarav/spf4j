package org.spf4j.junit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.spf4j.stackmonitor.FastStackCollector;
import org.spf4j.stackmonitor.Sampler;

/**
 * A Junit run listener that will collect performance profiles for your unit tests.
 *
 * Works only if unit tests are NOT executed in parallel inside the same JVM.
 *
 * To enable this Run listener with maven, you will need to configure your maven-surefire-plugin with:
 *
 *       <plugin>
 *       <groupId>org.apache.maven.plugins</groupId>
 *       <artifactId>maven-surefire-plugin</artifactId>
 *       <configuration>
 *         <properties>
 *           <property>
 *             <name>listener</name>
 *             <value>org.spf4j.junit.Spf4jRunListener</value>
 *           </property>
 *           <property>
 *             <name>spf4j.junit.sampleTimeMillis</name>
 *             <value>5</value>
 *           </property>
 *         </properties>
 *       </configuration>
 *     </plugin>
 *
 * and add spf4j-junit to your test classpath:
 *
 *   <dependency>
 *     <groupId>org.spf4j</groupId>
 *     <artifactId>spf4j-junit</artifactId>
 *     <version>${spf4j.version}</version>
 *     <scope>test</scope>
 *   </dependency>
 *
 * @author zoly
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
@NotThreadSafe
public final class Spf4jRunListener extends RunListener {

  private final Sampler sampler;

  private final File destinationFolder;

  private volatile File lastWrittenFile;

  public Spf4jRunListener() {
    sampler = new Sampler(Integer.getInteger("spf4j.junit.sampleTimeMillis", 5),
          Integer.getInteger("spf4j.junit.dumpAfterMillis", Integer.MAX_VALUE),
          new FastStackCollector(true));

    destinationFolder = new File(System.getProperty("spf4j.junit.destinationFolder",
          "target/junit-ssdump"));

    if (!destinationFolder.mkdirs() && !destinationFolder.canWrite()) {
      throw new ExceptionInInitializerError("Unable to write to " + destinationFolder);
    }
  }

  @Override
  public void testFailure(final Failure failure)
          throws IOException, InterruptedException {
    sampler.stop();
    File dumpToFile = sampler.dumpToFile(new File(destinationFolder, failure.getTestHeader() + ".ssdump2"));
    if (dumpToFile != null) {
      System.out.print("Profile saved to " + dumpToFile);
    }
  }

  @Override
  public void testFinished(final Description description)
          throws IOException, InterruptedException {
    sampler.stop();
    File dump = sampler.dumpToFile(new File(destinationFolder, description.getDisplayName() + ".ssdump2"));
    if (dump != null) {
      System.out.print("Profile saved to " + dump);
      lastWrittenFile = dump;
    }
  }

  @Override
  public void testStarted(final Description description) {
    sampler.start();
  }

  public Sampler getSampler() {
    return sampler;
  }

  public File getDestinationFolder() {
    return destinationFolder;
  }

  public File getLastWrittenFile() {
    return lastWrittenFile;
  }


  @Override
  public String toString() {
    return "Spf4jRunListener{" + "sampler=" + sampler + '}';
  }

}
