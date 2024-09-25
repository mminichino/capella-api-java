package com.codelry.util.capella;

import com.codelry.util.capella.logic.UserData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CapellaTest {
  public String profileName = "pytest";
  public String projectName = "pytest-project";

  @Test
  public void testCapella1() {
    CapellaProject project = new CapellaProject(projectName, profileName).getProject();
    Assertions.assertNotNull(project);
  }
}
