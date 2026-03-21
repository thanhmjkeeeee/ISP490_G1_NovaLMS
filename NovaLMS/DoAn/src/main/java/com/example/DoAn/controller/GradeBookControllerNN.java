package com.example.DoAn.controller;

public class GradeBookControllerNN {

    //Nhap
//    @Operation(summary = "Add new class")
//    @PostMapping("/")
//    public ResponseData<Integer> addClass(@Valid @RequestBody ClassRequestDTO request) {
//        try {
//            Integer classId = classService.saveClass(request);
//            return new ResponseData<>(HttpStatus.CREATED.value(), "Success", classId);
//        } catch (Exception e) {
//            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
//        }
//    }
//
//    @Operation(summary = "Update class info")
//    @PutMapping("/{id}")
//    public ResponseData<Void> updateClass(@PathVariable Integer id, @Valid @RequestBody ClassRequestDTO request) {
//        try {
//            classService.updateClass(id, request);
//            return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Updated");
//        } catch (Exception e) {
//            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
//        }
//    }
//
//    @Operation(summary = "Get list of classes with pagination")
//    @GetMapping("/list")
//    public ResponseData<PageResponse<?>> getList(
//            @RequestParam(defaultValue = "0") int pageNo,
//            @RequestParam(defaultValue = "10") int pageSize) {
//        try {
//            return new ResponseData<>(HttpStatus.OK.value(), "Success", classService.getAllClasses(pageNo, pageSize));
//        } catch (Exception e) {
//            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
//        }
//
//
//
//    private final UserRepository userRepository;
//    private final SettingRepository settingRepository;
//    private final EmailVerificationRepository verificationRepository;
//    private final PasswordResetTokenRepository tokenRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtUtil jwtUtil;
//    private final JavaMailSender mailSender;
//    private final AuthenticationManager authenticationManager;
//    private final PasswordResetTokenRepository passwordResetTokenRepository;
//
//    @Value("${app.frontend.url}")
//    private String frontendUrl;
//
//    @Override
//    public ResponseData<LoginResponse> login(LoginRequest request) {
//        authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
//        );
//
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại."));
//
//        if (!"Active".equalsIgnoreCase(user.getStatus())) {
//            throw new DisabledException("Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt.");
//        }
//
//        String token = jwtUtil.generateToken(user);
//
//        return ResponseData.success("Đăng nhập thành công",
//                LoginResponse.builder()
//                        .token(token)
//                        .email(user.getEmail())
//                        .fullName(user.getFullName())
//                        .role(user.getRole().getValue())
//                        .build());
//    }
//
//    @Override
//    @Transactional
//    public ResponseData<Void> registerUser(RegisterRequestDTO request) {
//        if (!request.getPassword().equals(request.getConfirmPassword())) {
//            throw new InvalidDataException("Mật khẩu xác nhận không khớp!");
//        }
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new InvalidDataException("Email này đã được sử dụng!");
//        }
//
//        // Kiểm tra mã xác minh
//        EmailVerification verifyEntity = verificationRepository
//                .findFirstByEmailOrderByExpiryTimeDesc(request.getEmail())
//                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã xác minh cho email này."));
//
//        if (!verifyEntity.getVerificationCode().equals(request.getVerificationCode())) {
//            throw new InvalidDataException("Mã xác minh không chính xác!");
//        }
//        if (verifyEntity.isExpired()) {
//            throw new InvalidDataException("Mã xác minh đã hết hạn.");
//        }
//
//        // Lấy Role mặc định từ bảng Setting
//        Setting studentRole = settingRepository.findRoleByValue("ROLE_STUDENT")
//                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình hệ thống lỗi: Không tìm thấy Role học viên."));
//
//        User newUser = User.builder()
//                .email(request.getEmail())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .fullName(request.getFullName())
//                .mobile(request.getPhone())
//                .gender(request.getGender())
//                .city(request.getCity())
//                .role(studentRole)
//                .status("Active")
//                .authProvider("LOCAL")
//                .build();
//
//        userRepository.save(newUser);
//        verificationRepository.deleteByEmail(request.getEmail());
//
//        return ResponseData.success("Đăng ký tài khoản thành công!", null);
//    }
//
//    @Override
//    @Transactional
//    public ResponseData<Void> sendVerificationCode(String email) {
//        if (userRepository.existsByEmail(email)) {
//            throw new InvalidDataException("Email này đã được đăng ký trong hệ thống.");
//        }
//
//        String code = String.format("%06d", new Random().nextInt(1000000));
//
//        // Xóa mã cũ nếu có
//        verificationRepository.deleteByEmail(email);
//
//        EmailVerification verification = EmailVerification.builder()
//                .email(email)
//                .verificationCode(code)
//                .expiryTime(LocalDateTime.now().plusMinutes(5))
//                .build();
//
//        verificationRepository.save(verification);
//
//        sendEmail(email, "Mã xác minh Nova LMS", "Mã xác minh của bạn là: " + code + ". Hiệu lực trong 5 phút.");
//
//        return ResponseData.success("Mã xác minh đã được gửi vào Email của bạn.", null);
//    }
//
//    @Override
//    @Transactional
//    public ResponseData<Void> forgotPassword(ForgotPasswordRequest request) {
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new ResourceNotFoundException("Email không tồn tại trong hệ thống."));
//
//        tokenRepository.deleteByUser(user);
//
//        String tokenStr = UUID.randomUUID().toString();
//        PasswordResetToken token = PasswordResetToken.builder()
//                .user(user)
//                .token(tokenStr)
//                .expiryDatetime(LocalDateTime.now().plusMinutes(15))
//                .isUsed(false)
//                .build();
//
//        tokenRepository.save(token);
//
//        String resetLink = frontendUrl + "/reset-password.html?token=" + tokenStr;
//        sendEmail(user.getEmail(), "[Nova LMS] Đặt lại mật khẩu",
//                "Vui lòng nhấn vào link sau để đặt lại mật khẩu (Hết hạn sau 15 phút): \n" + resetLink);
//
//        return ResponseData.success("Link đặt lại mật khẩu đã được gửi.", null);
//    }
//
//    @Override
//    @Transactional
//    public ResponseData<Void> resetPassword(ResetPasswordRequest request) {
//        try {
//            // 1. Tìm token trong DB
//            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken()).orElse(null);
//
//            if (resetToken == null) {
//                return ResponseData.error(400, "Token không hợp lệ hoặc không tồn tại!");
//            }
//            if (resetToken.isUsed() || resetToken.getExpiryDatetime().isBefore(LocalDateTime.now())) {
//                return ResponseData.error(400, "Token đã hết hạn hoặc đã được sử dụng!");
//            }
//
//            User user = resetToken.getUser();
//
//            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
//                return ResponseData.error(400, "Mật khẩu mới không được trùng với mật khẩu hiện tại!");
//            }
//
//            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//            userRepository.save(user);
//
//            resetToken.setUsed(true);
//            passwordResetTokenRepository.save(resetToken);
//
//            return ResponseData.success("Đặt lại mật khẩu thành công!");
//
//        } catch (Exception e) {
//            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
//        }
//    }
//    private void sendEmail(String to, String subject, String content) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(to);
//        message.setSubject(subject);
//        message.setText(content);
//        mailSender.send(message);
//    }
//
}
