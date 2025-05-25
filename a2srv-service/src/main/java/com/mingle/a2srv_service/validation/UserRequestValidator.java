package com.mingle.a2srv_service.validation;

import com.mingle.a2srv_service.dto.UserRequest;
import io.micrometer.common.lang.NonNullApi;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@NonNullApi
public class UserRequestValidator implements Validator {

    private static final Set<String> ALLOWED_GENDERS = Set.of("male", "female", "non-binary", "prefer_not_to_say");
    private static final Set<String> ALLOWED_PREFERENCES = Set.of("male", "female", "everyone");
    private static final Set<String> ALLOWED_GOALS = Set.of("friendship", "serious", "casual");

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,}$");

    @Override
    public boolean supports(Class<?> clazz) {
        return UserRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UserRequest req = (UserRequest) target;

        if (req.getUsername() != null) {
            if (req.getUsername().isBlank()) {
                errors.rejectValue("username", "Blank.username", "Username cannot be blank");
            } else if (req.getUsername().length() > 50) {
                errors.rejectValue("username", "Length.username", "Username max length is 50");
            }
        }

        if (req.getEmail() != null) {
            if (!EMAIL_REGEX.matcher(req.getEmail()).matches()) {
                errors.rejectValue("email", "Format.email", "Invalid email format");
            }
        }

        if (req.getPassword() != null && req.getPassword().length() < 6) {
            errors.rejectValue("password", "Length.password", "Password must be at least 6 characters");
        }

        if (req.getName() != null && req.getName().length() > 50) {
            errors.rejectValue("name", "Length.name", "Name max length is 50");
        }

        if (req.getSurname() != null && req.getSurname().length() > 50) {
            errors.rejectValue("surname", "Length.surname", "Surname max length is 50");
        }

        if (req.getAge() != null && req.getAge() < 18) {
            errors.rejectValue("age", "Min.age", "Age must be at least 18");
        }

        if (req.getGender() != null && !ALLOWED_GENDERS.contains(req.getGender().toLowerCase().trim())) {
            errors.rejectValue("gender", "Invalid.gender", "Invalid gender value");
        }

        if (req.getDatePreference() != null &&
                !ALLOWED_PREFERENCES.contains(req.getDatePreference().toLowerCase().trim())) {
            errors.rejectValue("datePreference", "Invalid.datePreference", "Invalid date preference value");
        }

        if (req.getRelationshipGoal() != null &&
                !ALLOWED_GOALS.contains(req.getRelationshipGoal().toLowerCase().trim())) {
            errors.rejectValue("relationshipGoal", "Invalid.relationshipGoal", "Invalid relationship goal");
        }

        if (req.getBio() != null && req.getBio().length() > 500) {
            errors.rejectValue("bio", "Length.bio", "Bio max length is 500");
        }

        if (req.getPhotoUrl() != null && req.getPhotoUrl().length() > 255) {
            errors.rejectValue("photoUrl", "Length.photoUrl", "Photo URL too long");
        }

        if (req.getCity() != null && req.getCity().length() > 100) {
            errors.rejectValue("city", "Length.city", "City max length is 100");
        }

        if (req.getLocationLat() != null && (req.getLocationLat() < -90 || req.getLocationLat() > 90)) {
            errors.rejectValue("locationLat", "Range.lat", "Latitude must be between -90 and 90");
        }

        if (req.getLocationLon() != null && (req.getLocationLon() < -180 || req.getLocationLon() > 180)) {
            errors.rejectValue("locationLon", "Range.lon", "Longitude must be between -180 and 180");
        }

        if (req.getBirthDate() != null && req.getBirthDate().isAfter(LocalDate.now())) {
            errors.rejectValue("birthDate", "Future.birthDate", "Birth date cannot be in the future");
        }

        List<String> interests = req.getInterests();
        if (interests != null) {
            if (interests.size() > 20) {
                errors.rejectValue("interests", "Max.interests.size", "You can specify at most 20 interests");
            }
            if (interests.stream().anyMatch(i -> i == null || i.trim().isEmpty())) {
                errors.rejectValue("interests", "Invalid.interests", "Interest items cannot be empty");
            }
        }
    }
}
