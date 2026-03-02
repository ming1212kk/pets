package com.pet.social.controller;

import com.pet.social.config.AuthInterceptor;
import com.pet.social.domain.PetProfile;
import com.pet.social.domain.UserAccount;
import com.pet.social.store.InMemoryDataStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pets")
public class PetController {
    private final InMemoryDataStore dataStore;

    public PetController(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping
    public List<PetResponse> listPets(HttpServletRequest request) {
        UserAccount actor = currentUser(request);
        return dataStore.listPetsByOwner(actor.getId()).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public PetResponse createPet(@Valid @RequestBody PetUpsertRequest request, HttpServletRequest httpRequest) {
        UserAccount actor = currentUser(httpRequest);
        PetProfile pet = dataStore.createPet(actor.getId(), request.name(), request.type(), request.ageMonths());
        return toResponse(pet);
    }

    @PutMapping("/{petId}")
    public PetResponse updatePet(@PathVariable long petId, @Valid @RequestBody PetUpsertRequest request, HttpServletRequest httpRequest) {
        UserAccount actor = currentUser(httpRequest);
        PetProfile pet = dataStore.updatePet(actor.getId(), petId, request.name(), request.type(), request.ageMonths());
        return toResponse(pet);
    }

    @PostMapping("/{petId}/default")
    public ActionResponse setDefaultPet(@PathVariable long petId, HttpServletRequest request) {
        UserAccount actor = currentUser(request);
        dataStore.setDefaultPet(actor.getId(), petId);
        return new ActionResponse("默认宠物设置成功");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    private PetResponse toResponse(PetProfile pet) {
        return new PetResponse(
            pet.getId(),
            pet.getName(),
            pet.getType(),
            pet.getAgeMonths(),
            pet.isDefaultPet(),
            pet.getCreatedAt().toString(),
            pet.getUpdatedAt().toString()
        );
    }

    private UserAccount currentUser(HttpServletRequest request) {
        return (UserAccount) request.getAttribute(AuthInterceptor.CURRENT_USER);
    }

    public record PetUpsertRequest(@NotBlank String name, @NotBlank String type, @NotNull Integer ageMonths) {
    }

    public record PetResponse(long id, String name, String type, Integer ageMonths, boolean defaultPet, String createdAt,
                              String updatedAt) {
    }

    public record ActionResponse(String message) {
    }

    public record ErrorResponse(String message) {
    }
}
