package com.SmartContactManager.Controllers;

import com.SmartContactManager.Entites.Contact;
import com.SmartContactManager.Entites.User;
import com.SmartContactManager.Repos.ContactRepo;
import com.SmartContactManager.Repos.UserRepo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Controller
public class homeController {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ContactRepo contactRepo;

    @GetMapping("/home")
    public String homeView(){
        return "home";
    }

    @GetMapping("/about")
    public String aboutView(){
        return "about";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";   // register.html inside templates

    }

    @PostMapping("/register")
    public String processRegister(
            @ModelAttribute("user") User user,
            @RequestParam("profileImage") MultipartFile profileImage,
            Model model) {

        // 🔹 check duplicate email
        if (userRepo.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email already exists");
            return "register";
        }

        // Encrypt password BEFORE saving
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);


        // 🔹 default role & enabled
        user.setRole("ROLE_USER");
        user.setEnabled(true);

        // 🔹 profile image handling (only filename for now)
        if (profileImage != null && !profileImage.isEmpty()) {
            user.setImageUrl(profileImage.getOriginalFilename());
        } else {
            user.setImageUrl("default.png");
        }

        // 🔹 save into DB
        userRepo.save(user);

        model.addAttribute("message", "User Registered Successfully");
        model.addAttribute("user", new User());

        // Redirect to login page
        return "redirect:/login";
    }


    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("user", new User());
        return "login";   // register.html inside templates

    }


    public String loginUser(
            @RequestParam("username") String email,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes
    ) {
        User user = userRepo.findByEmail(email);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
            System.out.println("Not Login");
            return "redirect:/login";
        }

        System.out.println("Login success");

        // Set authentication manually
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        redirectAttributes.addFlashAttribute("message", "Welcome " + user.getName() + "!");
        return "redirect:/dashboard";
    }


// dashboard

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = "User";
        String role = "GUEST";

        if (authentication != null && authentication.isAuthenticated()) {

            Object principal = authentication.getPrincipal();

            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {

                username = userDetails.getUsername();
                role = userDetails.getAuthorities()
                        .stream()
                        .findFirst()
                        .map(Object::toString)
                        .orElse("USER");
            }
            else if (principal instanceof String p && !p.equals("anonymousUser")) {
                username = p;
            }
        }

        // ========= Dashboard values ==========
        model.addAttribute("username", username);
        model.addAttribute("role", role);

        model.addAttribute("successMessage", "Welcome back, " + username + "!");
        model.addAttribute("totalContacts", 25);
        model.addAttribute("pendingTasks", 5);
        model.addAttribute("profileStatus", "Active");

        model.addAttribute("showDashboardLink", true);
        model.addAttribute("isAdmin", role.equalsIgnoreCase("ADMIN"));

        // ========= Dynamic Navbar for Dashboard ==========
        List<Map<String, String>> navLinks = new ArrayList<>();

        navLinks.add(Map.of("title", "Contacts", "url", "/contact_list"));
        navLinks.add(Map.of("title", "Emails", "url", "/emails"));
        navLinks.add(Map.of("title", "My Profile", "url", "/profile"));
        navLinks.add(Map.of("title", "Dashboard", "url", "/dashboard"));

        model.addAttribute("navLinks", navLinks);

        // Dropdown (optional)
        List<Map<String, String>> dropdownLinks = new ArrayList<>();
        dropdownLinks.add(Map.of("title", "Settings", "url", "/settings"));
        dropdownLinks.add(Map.of("title", "Pending Tasks", "url", "/pending_tasks"));

        // Admin-only items
        if (role.equalsIgnoreCase("ADMIN")) {
            dropdownLinks.add(Map.of("title", "User Management", "url", "/admin/users"));
        }

        model.addAttribute("dropdownLinks", dropdownLinks);


        // add searchbar

        model.addAttribute("searchEnabled", true);

        return "dashboard";
    }


    @GetMapping("/contact-add")
    public String addContactForm(Model model) {
        model.addAttribute("contact", new Contact());
        return "Contact-add";
    }

    // controller save the contact

    @PostMapping("/contact-save")
    public String saveContact(
            @Valid @ModelAttribute("contact") Contact contact,
            BindingResult result,
            @RequestParam("imageFile") MultipartFile file,
            Principal principal,
            Model model) {

        // Backend phone validation
        if (!contact.getPhoneno().matches("\\d{10}")) {
            model.addAttribute("error", "Phone number must be exactly 10 digits");
            return "contact-add";   // ⚠️ Return same page name
        }

        // Bean validation errors
        if (result.hasErrors()) {
            return "contact-add";   // ⚠️ Should return same page
        }

        // Check duplicate phone for same user (better)
        User user = userRepo.findByEmail(principal.getName());



        if (contactRepo.existsByPhonenoAndUser(contact.getPhoneno(), user)) {
            model.addAttribute("error", "Phone number already exists!");
            return "contact-add";
        }


        // Set relation both sides
        contact.setUser(user);
        user.getContacts().add(contact);

        // Image handling
        if (!file.isEmpty()) {
            contact.setImage(file.getOriginalFilename());
            // TODO: save file to uploads folder
        } else {
            contact.setImage("default.png");
        }

        // Save Contact
        contactRepo.save(contact);

        // Reset form & show success
        model.addAttribute("success", "Contact added successfully!");
        model.addAttribute("contact", new Contact());

        return "contact-add";   // <-- should match form page
    }


    @GetMapping("/contact-list")
    public String getUserContacts(Model model, Principal principal) {

        // Get logged-in username / email
        String username = principal.getName();

        User user = userRepo.findByEmail(username);

        List<Contact> contacts = contactRepo.findByUserId(user.getId());
        model.addAttribute("contacts", contacts);
        model.addAttribute("user", user);

        return "contacts-list";
    }

   // return update from

    @GetMapping("/contact-update/{id}")
    public String showUpdateForm(@PathVariable("id") Long contactId,
                                 Principal principal,
                                 Model model) {

        // Get currently logged-in user
        User user = userRepo.findByEmail(principal.getName());

        // Find contact by id
        Contact contact = contactRepo.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid contact ID: " + contactId));

        // ⚠ Check if this contact belongs to this user
        if (!contact.getUser().getId().equals(user.getId())) {
            model.addAttribute("error", "You are not allowed to update this contact!");
            return "redirect:/dashboard";  // or some error page
        }

        // Add contact to model
        model.addAttribute("contact", contact);

        return "contact-update";  // your Thymeleaf update form
    }

    //handle update

    @PostMapping("/contact-update/{id}")
    public String updateContact(@PathVariable("id") Long contactId,
                                @Valid @ModelAttribute("contact") Contact contact,
                                BindingResult result,
                                @RequestParam("imageFile") MultipartFile file,
                                Principal principal,
                                Model model) {

        User user = userRepo.findByEmail(principal.getName());

        Contact existingContact = contactRepo.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid contact ID: " + contactId));

        // Ensure the user owns this contact
        if (!existingContact.getUser().getId().equals(user.getId())) {
            model.addAttribute("error", "You are not allowed to update this contact!");
            return "redirect:/dashboard";
        }

        // Validation
        if (!contact.getPhoneno().matches("\\d{10}")) {
            model.addAttribute("error", "Phone number must be exactly 10 digits");
            return "contact-update";
        }

        if (result.hasErrors()) {
            return "contact-update";
        }

        // Check for duplicate phone for same user, excluding this contact
        if (contactRepo.existsByPhonenoAndUserAndIdNot(contact.getPhoneno(), user, contactId)) {
            model.addAttribute("error", "Phone number already exists!");
            return "contact-update";
        }

        // Update fields
        existingContact.setName(contact.getName());
        existingContact.setNickname(contact.getNickname());
        existingContact.setWork(contact.getWork());
        existingContact.setDescription(contact.getDescription());
        existingContact.setPhoneno(contact.getPhoneno());

        // Image
        if (!file.isEmpty()) {
            existingContact.setImage(file.getOriginalFilename());
            // TODO: save file
        }

        contactRepo.save(existingContact);

        model.addAttribute("success", "Contact updated successfully!");
        return "contact-update";
    }


    // delete success return






    // delete oparation

    @GetMapping("/contact-delete/{id}")
    public String deleteContactImpl(@PathVariable("id") Long contactId, Principal principal, Model model) {

        try {
            User user = userRepo.findByEmail(principal.getName());
            Optional<Contact> contactOpt = contactRepo.findById(contactId);

            if (contactOpt.isPresent() && contactOpt.get().getUser().getId().equals(user.getId())) {
                contactRepo.delete(contactOpt.get());
                model.addAttribute("success", "Contact deleted successfully!");
            } else {
                model.addAttribute("error", "Contact not found or you are not authorized to delete it!");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Error occurred while deleting the contact!");
        }

        return "contact-delete"; // Thymeleaf page
    }
}








