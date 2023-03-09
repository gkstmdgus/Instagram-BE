package instagram.instagrambe.user.service;

import instagram.instagrambe.jwt.JwtUtil;
import instagram.instagrambe.user.dto.CheckIdDto;
import instagram.instagrambe.user.dto.SignupRequestDto;
import instagram.instagrambe.user.entity.User;
import instagram.instagrambe.user.entity.UserRoleEnum;
import instagram.instagrambe.user.repository.UserRepository;
import instagram.instagrambe.util.CustomException;
import instagram.instagrambe.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${admin.token}")
    private String ADMIN_TOKEN;


    @Transactional
    public void checkId(CheckIdDto checkIdDto){
        String username = checkIdDto.getUsername();
        Optional<User> found = userRepository.findByUsername(username);

        if(found.isPresent()){
            throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
        }
    }

    @Transactional
    public void signup(SignupRequestDto requestDto, String username, String nickname, String email, String password, String adminToken) {
        UserRoleEnum role = UserRoleEnum.USER;
        if(requestDto.isAdmin()){
            if(!adminToken.equals(ADMIN_TOKEN)){
                throw new CustomException(ErrorCode.FORBIDDEN_DATA);
            }
            role = UserRoleEnum.ADMIN;
        }

        User user = new User(username, nickname, email, password, role);
        userRepository.save(user);
    }

    @Transactional
    public void login(String usernameOrEmail, UserRoleEnum Role, HttpServletResponse response) {
//        String username = requestDto.getUsername();
        Optional<User> user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        response.addHeader(jwtUtil.AUTHORIZATION_HEADER,jwtUtil.createToken(user.get().getUsername(), Role));
    }
}