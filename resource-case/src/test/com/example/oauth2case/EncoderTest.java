package com.example.oauth2case;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EncoderTest {

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Test
    public void encoderPassword(){
        System.out.println(passwordEncoder.encode("123456"));
    }
}
