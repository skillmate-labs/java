package com.skillmate.skillmate.modules.goals.useCases;

import org.springframework.stereotype.Service;

import com.skillmate.skillmate.modules.goals.dto.AIGoalSuggestionDTO;

@Service
public class GetAIGoalSuggestionUseCase {

  public AIGoalSuggestionDTO execute(String userExperience, String desiredSkill) {
    AIGoalSuggestionDTO suggestion = new AIGoalSuggestionDTO();
    suggestion.setTitle("Learning Goal for " + desiredSkill);
    suggestion.setExperience(
        String.format("Based on your experience: %s. This goal will help you master %s through structured learning.",
            userExperience, desiredSkill));

    int suggestedHours = userExperience.length() < 50 ? 3 : 2;
    int suggestedDays = userExperience.length() < 50 ? 6 : 5;

    suggestion.setSuggestedHoursPerDay(suggestedHours);
    suggestion.setSuggestedDaysPerWeek(suggestedDays);
    suggestion.setReasoning(
        String.format(
            "Suggested based on your experience level. For '%s', we recommend %d hours per day, %d days per week.",
            desiredSkill, suggestedHours, suggestedDays));

    return suggestion;
  }
}
