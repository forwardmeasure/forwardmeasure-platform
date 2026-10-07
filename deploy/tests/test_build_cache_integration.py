# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
# 
#     https://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Opt-in real Maven cache regression. Run only when build/test execution is authorized."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[2]
WRAPPER = ROOT.parent / "forwardmeasure-openworkflow/scripts/build-bounded.sh"


class BuildCacheIntegrationTest(unittest.TestCase):
    def test_restore_invalidation_and_tests_after_a_skipped_build(self):
        with tempfile.TemporaryDirectory(prefix="forwardmeasure-cache-fixture-") as directory:
            project = Path(directory)
            (project / ".mvn").mkdir()
            shutil.copy(ROOT / ".mvn/extensions.xml", project / ".mvn/extensions.xml")
            config = (ROOT / ".mvn/maven-build-cache-config.xml").read_text()
            config = config.replace("<local>", f"<local><location>{project / 'cache'}</location>")
            (project / ".mvn/maven-build-cache-config.xml").write_text(config)
            (project / "pom.xml").write_text(f'''<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion><groupId>com.forwardmeasure.fixture</groupId>
              <artifactId>cache-{uuid.uuid4().hex}</artifactId><version>1</version>
              <properties><maven.compiler.release>25</maven.compiler.release>
                <project.build.outputTimestamp>2026-10-06T00:00:00Z</project.build.outputTimestamp>
                <maven.build.cache.input.jdk>${{java.home}}/release</maven.build.cache.input.jdk></properties>
              <dependencies><dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId>
                <version>6.1.2</version><scope>test</scope></dependency></dependencies>
              <build><finalName>fixture</finalName><plugins>
                <plugin><artifactId>maven-clean-plugin</artifactId><version>3.5.0</version></plugin>
                <plugin><artifactId>maven-resources-plugin</artifactId><version>3.3.1</version></plugin>
                <plugin><artifactId>maven-compiler-plugin</artifactId><version>3.15.0</version></plugin>
                <plugin><artifactId>maven-jar-plugin</artifactId><version>3.4.2</version></plugin>
                <plugin><artifactId>maven-surefire-plugin</artifactId><version>3.5.4</version>
                  <configuration><systemPropertyVariables><fixture.marker>{project / 'target/executed'}</fixture.marker>
                  </systemPropertyVariables></configuration></plugin>
              </plugins></build></project>''')
            source = project / "src/main/java/Fixture.java"
            source.parent.mkdir(parents=True)
            source.write_text('public class Fixture { public static int answer() { return 42; } }')
            resource = project / "src/main/resources/schema.proto"
            resource.parent.mkdir(parents=True)
            resource.write_text('syntax = "proto3"; message First {}')
            test = project / "src/test/java/FixtureTest.java"
            test.parent.mkdir(parents=True)
            test.write_text('''import org.junit.jupiter.api.Test;
              import static org.junit.jupiter.api.Assertions.assertEquals;
              import java.nio.file.Files; import java.nio.file.Path;
              class FixtureTest { @Test void executes() throws Exception {
                assertEquals(42, Fixture.answer());
                Files.writeString(Path.of(System.getProperty("fixture.marker")), "executed");
              }}''')

            def build(skip):
                result = subprocess.run([str(WRAPPER), "-f", str(project / "pom.xml"), "-B", "-ntp",
                                         "clean", "package", "-Dmaven.test.skip=false",
                                         f"-DskipTests={str(skip).lower()}"], capture_output=True, text=True)
                self.assertEqual(0, result.returncode, result.stdout + result.stderr)
                return result.stdout

            build(True)
            self.assertFalse((project / "target/executed").exists())
            initial = (project / "target/fixture.jar").read_bytes()
            warm_log = build(True)
            self.assertIn("Found cached build", warm_log)
            self.assertEqual(initial, (project / "target/fixture.jar").read_bytes())
            self.assertTrue((project / "target/classes/Fixture.class").is_file())
            self.assertTrue((project / "target/test-classes/FixtureTest.class").is_file())
            build(False)
            self.assertEqual("executed", (project / "target/executed").read_text())
            resource.write_text('syntax = "proto3"; message Changed {}')
            build(True)
            with zipfile.ZipFile(project / "target/fixture.jar") as jar:
                self.assertEqual(resource.read_bytes(), jar.read("schema.proto"))



    def test_compile_only_cache_restore_keeps_reactor_artifacts_and_generated_source_roots(self):
        with tempfile.TemporaryDirectory(prefix="forwardmeasure-cache-reactor-") as directory:
            project = Path(directory)
            (project / ".mvn").mkdir()
            shutil.copy(ROOT / ".mvn/extensions.xml", project / ".mvn/extensions.xml")
            config = (ROOT / ".mvn/maven-build-cache-config.xml").read_text()
            config = config.replace("<local>", f"<local><location>{project / 'cache'}</location>")
            (project / ".mvn/maven-build-cache-config.xml").write_text(config)
            artifact = "cache-reactor-" + uuid.uuid4().hex
            (project / "pom.xml").write_text(f"""<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion><groupId>com.forwardmeasure.fixture</groupId>
              <artifactId>{artifact}</artifactId><version>1</version><packaging>pom</packaging>
              <modules><module>library</module><module>consumer</module></modules>
              <properties><maven.compiler.release>25</maven.compiler.release></properties>
              <build><pluginManagement><plugins>
                <plugin><artifactId>maven-clean-plugin</artifactId><version>3.5.0</version></plugin>
                <plugin><artifactId>maven-resources-plugin</artifactId><version>3.3.1</version></plugin>
                <plugin><artifactId>maven-compiler-plugin</artifactId><version>3.15.0</version></plugin>
                <plugin><artifactId>maven-jar-plugin</artifactId><version>3.4.2</version></plugin>
                <plugin><artifactId>maven-javadoc-plugin</artifactId><version>3.12.0</version></plugin>
                <plugin><groupId>org.codehaus.mojo</groupId><artifactId>build-helper-maven-plugin</artifactId><version>3.6.1</version></plugin>
                <plugin><artifactId>maven-surefire-plugin</artifactId><version>3.5.4</version></plugin>
              </plugins></pluginManagement></build></project>""")
            parent = f"""<parent><groupId>com.forwardmeasure.fixture</groupId>
              <artifactId>{artifact}</artifactId><version>1</version></parent>"""

            def write(path, content):
                file = project / path
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_text(content)

            write("library/pom.xml", f"""<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>{parent}<artifactId>library</artifactId>
              <build><plugins><plugin><artifactId>maven-resources-plugin</artifactId>
                <executions><execution><id>fixture-generated-source</id><phase>generate-sources</phase>
                  <goals><goal>copy-resources</goal></goals><configuration>
                    <outputDirectory>DOLLAR{{project.build.directory}}/generated-sources/fixture</outputDirectory>
                    <resources><resource><directory>src/generator</directory></resource></resources>
                  </configuration></execution></executions></plugin>
                <plugin><groupId>org.codehaus.mojo</groupId><artifactId>build-helper-maven-plugin</artifactId>
                  <executions><execution><id>fixture-generated-root</id><phase>generate-sources</phase>
                    <goals><goal>add-source</goal></goals><configuration><sources>
                      <source>DOLLAR{{project.build.directory}}/generated-sources/fixture</source>
                    </sources></configuration></execution></executions></plugin>
                <plugin><artifactId>maven-javadoc-plugin</artifactId><executions>
                  <execution><id>docs</id><phase>package</phase><goals><goal>jar</goal></goals></execution>
                </executions></plugin></plugins></build></project>""".replace("DOLLAR", "$"))
            write("library/src/generator/api/GeneratedValue.java",
                  "package api; public class GeneratedValue { public static int answer() { return 42; } }")
            write("library/src/main/java/api/Service.java",
                  "package api; public class Service { public static int answer() { return GeneratedValue.answer(); } }")
            write("consumer/pom.xml", f"""<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>{parent}<artifactId>consumer</artifactId>
              <dependencies><dependency><groupId>com.forwardmeasure.fixture</groupId>
                <artifactId>library</artifactId><version>1</version></dependency>
                <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId>
                  <version>6.1.2</version><scope>test</scope></dependency></dependencies></project>""")
            write("consumer/src/test/java/ConsumerTest.java", """import org.junit.jupiter.api.Test;
              import static org.junit.jupiter.api.Assertions.assertEquals;
              class ConsumerTest { @Test void resolvesGeneratedDependency() throws Exception {
                assertEquals(42, api.Service.answer());
                java.nio.file.Files.writeString(java.nio.file.Path.of("target/executed"), "executed");
              }}""")

            def build(*goals):
                result = subprocess.run([str(WRAPPER), "-f", str(project / "pom.xml"), "-B", "-ntp",
                                         "-DskipTests=false", *goals], capture_output=True, text=True,
                                        timeout=180)
                self.assertEqual(0, result.returncode, result.stdout + result.stderr)
                return result.stdout

            build("test")
            warm = build("clean", "test")
            self.assertIn("Found cached build", warm)
            self.assertEqual("executed", (project / "consumer/target/executed").read_text())
            self.assertTrue((project / "library/target/classes/api/GeneratedValue.class").exists())
            build("package")
            self.assertTrue((project / "library/target/library-1-javadoc.jar").exists())


    def test_openapi_generated_dependency_survives_warm_reactor_compile(self):
        with tempfile.TemporaryDirectory(prefix="forwardmeasure-cache-openapi-") as directory:
            project = Path(directory)
            def write(path, content):
                file = project / path
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_text(content)
            write(".mvn/extensions.xml", (ROOT / ".mvn/extensions.xml").read_text())
            config = (ROOT / ".mvn/maven-build-cache-config.xml").read_text()
            config = config.replace("<local>", f"<local><location>{project / 'cache'}</location>")
            write(".mvn/maven-build-cache-config.xml", config)
            artifact = "openapi-cache-" + uuid.uuid4().hex
            write("pom.xml", f"""<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion><groupId>com.forwardmeasure.fixture</groupId>
              <artifactId>{artifact}</artifactId><version>1</version><packaging>pom</packaging>
              <modules><module>model</module><module>consumer</module></modules>
              <properties><maven.compiler.release>25</maven.compiler.release></properties>
              <build><pluginManagement><plugins>
                <plugin><artifactId>maven-clean-plugin</artifactId><version>3.5.0</version></plugin>
                <plugin><artifactId>maven-resources-plugin</artifactId><version>3.3.1</version></plugin>
                <plugin><artifactId>maven-compiler-plugin</artifactId><version>3.15.0</version></plugin>
                <plugin><artifactId>maven-surefire-plugin</artifactId><version>3.5.4</version></plugin>
              </plugins></pluginManagement></build></project>""")
            parent = f"""<parent><groupId>com.forwardmeasure.fixture</groupId>
              <artifactId>{artifact}</artifactId><version>1</version></parent>"""
            write("model/pom.xml", f"""<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>{parent}<artifactId>model</artifactId>
              <dependencies>
                <dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-annotations</artifactId><version>2.22</version></dependency>
                <dependency><groupId>jakarta.annotation</groupId><artifactId>jakarta.annotation-api</artifactId><version>3.0.0</version></dependency>
              </dependencies>
              <build><plugins><plugin><groupId>org.openapitools</groupId>
                <artifactId>openapi-generator-maven-plugin</artifactId><version>7.25.0</version>
                <executions><execution><goals><goal>generate</goal></goals><configuration>
                  <inputSpec>${{project.basedir}}/src/main/openapi/schema.yaml</inputSpec>
                  <generatorName>jaxrs-spec</generatorName><modelPackage>fixture.generated</modelPackage>
                  <generateApis>false</generateApis><generateSupportingFiles>false</generateSupportingFiles>
                  <generateModelTests>false</generateModelTests><generateModelDocumentation>false</generateModelDocumentation>
                  <configOptions><sourceFolder>src/main/java</sourceFolder><useJakartaEe>true</useJakartaEe><useBeanValidation>false</useBeanValidation>
                    <useSwaggerAnnotations>false</useSwaggerAnnotations><openApiNullable>false</openApiNullable>
                    <hideGenerationTimestamp>true</hideGenerationTimestamp></configOptions>
                </configuration></execution></executions>
              </plugin></plugins></build></project>""")
            write("model/src/main/openapi/schema.yaml", """openapi: 3.0.3
info: {title: Cache fixture, version: '1'}
paths: {}
components:
  schemas:
    GeneratedValue:
      type: object
      properties:
        answer: {type: integer, format: int32}
""")
            write("consumer/pom.xml", f"""<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>{parent}<artifactId>consumer</artifactId>
              <dependencies><dependency><groupId>com.forwardmeasure.fixture</groupId><artifactId>model</artifactId><version>1</version></dependency>
              <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><version>6.1.2</version><scope>test</scope></dependency>
              </dependencies></project>""")
            write("consumer/src/test/java/GeneratedContractTest.java", """import org.junit.jupiter.api.Test;
              import static org.junit.jupiter.api.Assertions.assertEquals;
              class GeneratedContractTest { @Test void generatedModelRemainsUsable() throws Exception {
                assertEquals(42, new fixture.generated.GeneratedValue().answer(42).getAnswer());
                java.nio.file.Files.writeString(java.nio.file.Path.of("target/executed"), "executed");
              }}""")
            for cold in (True, False):
                if not cold:
                    # Force a genuinely fresh downstream compile against the cached generated module.
                    write("consumer/src/test/java/NewConsumerTest.java", """import org.junit.jupiter.api.Test;
                      import static org.junit.jupiter.api.Assertions.assertEquals;
                      class NewConsumerTest { @Test void newConsumerUsesCachedModel() {
                        assertEquals(7, new fixture.generated.GeneratedValue().answer(7).getAnswer());
                      }}""")
                result = subprocess.run([str(WRAPPER), "-f", str(project / "pom.xml"), "-B", "-ntp",
                                         *( ["clean"] if cold else [] ), "test", "-Dmaven.build.cache.enabled=true"],
                                        capture_output=True, text=True, timeout=180)
                self.assertEqual(0, result.returncode, result.stdout + result.stderr)
                if not cold:
                    self.assertIn("Found cached build", result.stdout)
                self.assertTrue((project / "model/target/classes/fixture/generated/GeneratedValue.class").is_file())
                self.assertEqual("executed", (project / "consumer/target/executed").read_text())


if __name__ == "__main__":
    unittest.main()
