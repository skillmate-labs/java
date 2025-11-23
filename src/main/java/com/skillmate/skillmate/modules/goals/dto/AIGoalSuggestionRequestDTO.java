package com.skillmate.skillmate.modules.goals.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIGoalSuggestionRequestDTO {
  @NotBlank(message = "Experience is required")
  private String experience;

  @NotBlank(message = "Skill is required")
  private String skill;
}
