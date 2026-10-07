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

"""Real Maven proof that generated exemptions do not exempt handwritten classes."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import uuid
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
WRAPPER = ROOT.parent / "forwardmeasure-openworkflow/scripts/build-bounded.sh"


class GeneratedCoverageIntegrationTest(unittest.TestCase):
    def test_generated_exemption_and_handwritten_failure_in_the_same_package(self):
        with tempfile.TemporaryDirectory(prefix="forwardmeasure-generated-coverage-") as directory:
            project = Path(directory)
            (project / ".mvn").mkdir()
            shutil.copy(ROOT / ".mvn/extensions.xml", project / ".mvn/extensions.xml")
            config = (ROOT / ".mvn/maven-build-cache-config.xml").read_text()
            config = config.replace("<local>", f"<local><location>{project / 'cache'}</location>")
            (project / ".mvn/maven-build-cache-config.xml").write_text(config)
            (project / "pom.xml").write_text(f"""<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <parent><groupId>com.forwardmeasure.platform</groupId>
                <artifactId>forwardmeasure-platform</artifactId><version>1.1.0</version>
                <relativePath>{ROOT / 'pom.xml'}</relativePath></parent>
              <groupId>com.forwardmeasure.fixture</groupId><artifactId>coverage-{uuid.uuid4().hex}</artifactId>
              <version>1</version>
              <dependencies><dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId>
                <scope>test</scope></dependency></dependencies>
              <build><plugins>
                <plugin><artifactId>maven-resources-plugin</artifactId><executions><execution>
                  <id>fixture-generation</id><phase>generate-sources</phase><goals><goal>copy-resources</goal></goals>
                  <configuration><outputDirectory>DOLLAR{{project.build.directory}}/generated-sources/fixture</outputDirectory>
                    <resources><resource><directory>src/generator</directory></resource></resources>
                  </configuration></execution></executions></plugin>
                <plugin><groupId>org.codehaus.mojo</groupId><artifactId>build-helper-maven-plugin</artifactId>
                  <executions><execution><id>generated-fixture-root</id><phase>generate-sources</phase>
                    <goals><goal>add-source</goal></goals><configuration><sources>
                      <source>DOLLAR{{project.build.directory}}/generated-sources/fixture</source>
                    </sources></configuration></execution></executions></plugin>
              </plugins></build></project>""".replace("DOLLAR", "$"))

            def write(path, content):
                destination = project / path
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_text(content)

            write("src/generator/same/GeneratedDto.java", """package same;
              public class GeneratedDto {
                public static int value(boolean flag) { return flag ? 1 : 0; }
              }""")
            write("src/main/java/same/Service.java", """package same;
              public class Service {
                public int sign(int input) { if (input >= 0) return 1; return -1; }
              }""")

            def test_source(both):
                write("src/test/java/same/ServiceTest.java", """package same;
                  import org.junit.jupiter.api.Test;
                  import static org.junit.jupiter.api.Assertions.assertEquals;
                  class ServiceTest { @Test void signPreservesNegativeAndNonnegativeInputs() {
                    var service = new Service(); assertEquals(1, service.sign(0));
                    """ + ("assertEquals(-1, service.sign(-1));" if both else "") + "}}")

            def build():
                result = subprocess.run(
                    [str(WRAPPER), "-f", str(project / "pom.xml"), "-B", "-ntp",
                     "-Pcoverage", "-Drat.skip=true", "-DskipTests=false", "-DskipITs=false",
                     "clean", "spotless:apply", "verify"], capture_output=True, text=True, timeout=180)
                return result.returncode, result.stdout + result.stderr

            test_source(True)
            code, log = build()
            self.assertEqual(0, code, log)
            report = ET.parse(project / "target/site/jacoco/jacoco.xml")
            classes = {node.attrib["name"] for node in report.findall(".//class")}
            self.assertEqual({"same/Service"}, classes)
            self.assertEqual(["same/GeneratedDto.class"],
                             (project / "target/coverage-generated-excludes.txt").read_text().splitlines())

            test_source(False)
            code, log = build()
            self.assertNotEqual(0, code, log)
            self.assertIn("branches covered ratio is 0.50", log)
            self.assertIn("expected minimum is 0.85", log)


if __name__ == "__main__":
    unittest.main()
