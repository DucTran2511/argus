package com.argus.api;

import com.argus.api.dto.LabelRequest;
import com.argus.api.dto.response.LabelResponse;
import com.argus.api.spec.AddressBookApi;
import com.argus.core.security.AuthContext;
import com.argus.core.security.AuthenticatedUser;
import com.argus.domain.model.AddressLabel;
import com.argus.domain.service.AddressBookService;
import com.argus.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/address-book")
@RequiredArgsConstructor
@Slf4j
public class AddressBookController implements AddressBookApi {

    private final AddressBookService addressBookService;
    private final UserService userService;

    private UUID getCurrentUserId() {
        AuthenticatedUser user = AuthContext.currentUser();
        return userService.getOrCreateUser(user.supabaseUid(), user.email()).getId();
    }

    @Override
    @PostMapping
    public ResponseEntity<?> addLabel(@Valid @RequestBody LabelRequest request) {
        log.info("POST /api/v1/address-book - Adding label '{}' to {}", request.getLabel(), request.getAddress());
        AddressLabel created = addressBookService.addLabel(
                request.getAddress(), request.getLabel(), request.getCategory(), getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(LabelResponse.fromDomain(created));
    }

    @Override
    @GetMapping("/{address}")
    public ResponseEntity<LabelResponse> getLabels(@PathVariable String address) {
        log.info("GET /api/v1/address-book/{} - Fetching labels", address);
        List<AddressLabel> labels = addressBookService.getLabels(address, getCurrentUserId());
        return ResponseEntity.ok(LabelResponse.fromDomainList(address, labels));
    }

    @Override
    @DeleteMapping("/{address}/labels/{label}")
    public ResponseEntity<Void> removeLabel(@PathVariable String address, @PathVariable String label) {
        log.info("DELETE /api/v1/address-book/{}/labels/{}", address, label);
        addressBookService.removeLabel(address, label, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<List<LabelResponse>> searchLabels(
            @RequestParam(required = false) String label,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category) {

        log.info("GET /api/v1/address-book - Search: label={}, q={}, category={}", label, q, category);
        List<AddressLabel> results = addressBookService.search(label, q, category, getCurrentUserId());
        return ResponseEntity.ok(LabelResponse.fromDomainListGrouped(results));
    }

    @Override
    @PostMapping("/import")
    public ResponseEntity<?> importLabels(@RequestBody List<LabelRequest> requests) {
        log.info("POST /api/v1/address-book/import - Importing {} labels", requests != null ? requests.size() : 0);
        List<AddressLabel> toImport = LabelRequest.toDomainList(requests);
        List<AddressLabel> imported = addressBookService.importLabels(toImport, getCurrentUserId());
        return ResponseEntity.ok(Map.of(
                "imported", imported.size(),
                "total", requests != null ? requests.size() : 0,
                "skipped", requests != null ? requests.size() - imported.size() : 0));
    }
}
