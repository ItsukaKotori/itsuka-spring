package org.itsuka.web.desensitization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 脱敏功能测试类
 *
 * @author Itsuka
 * @since 2023/12/10
 */
public class DesensitizationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testUsernameDesensitization() throws JsonProcessingException {
        UserInfo user = new UserInfo();
        user.setUsername("张三丰");
        user.setPhone("13812345678");
        user.setEmail("zhangsanfeng@example.com");
        user.setIdCard("110105199003072345");
        user.setBankCard("6222021234567890123");
        user.setPassword("password123");
        user.setAddress("北京市朝阳区建国门外大街1号");

        String json = objectMapper.writeValueAsString(user);
        System.out.println("用户名脱敏测试: " + json);

        // 验证用户名脱敏
        assertEquals("张******", user.getUsernameDesensitized());
    }

    @Test
    public void testPhoneDesensitization() throws JsonProcessingException {
        UserInfo user = new UserInfo();
        user.setPhone("13812345678");

        String json = objectMapper.writeValueAsString(user);
        System.out.println("手机号脱敏测试: " + json);

        // 验证手机号脱敏
        assertEquals("138****5678", user.getPhoneDesensitized());
    }

    @Test
    public void testEmailDesensitization() throws JsonProcessingException {
        UserInfo user = new UserInfo();
        user.setEmail("zhangsanfeng@example.com");

        String json = objectMapper.writeValueAsString(user);
        System.out.println("邮箱脱敏测试: " + json);

        // 验证邮箱脱敏
        assertEquals("z***********@example.com", user.getEmailDesensitized());
    }

    @Test
    public void testIdCardDesensitization() throws JsonProcessingException {
        UserInfo user = new UserInfo();
        user.setIdCard("110105199003072345");

        String json = objectMapper.writeValueAsString(user);
        System.out.println("身份证号脱敏测试: " + json);

        // 验证身份证号脱敏
        assertEquals("110105********2345", user.getIdCardDesensitized());
    }

    @Test
    public void testBankCardDesensitization() throws JsonProcessingException {
        UserInfo user = new UserInfo();
        user.setBankCard("6222021234567890123");

        String json = objectMapper.writeValueAsString(user);
        System.out.println("银行卡号脱敏测试: " + json);

        // 验证银行卡号脱敏
        assertEquals("622202********0123", user.getBankCardDesensitized());
    }

    @Test
    public void testPasswordDesensitization() throws JsonProcessingException {
        UserInfo user = new UserInfo();
        user.setPassword("password123");

        String json = objectMapper.writeValueAsString(user);
        System.out.println("密码脱敏测试: " + json);

        // 验证密码脱敏
        assertEquals("***********", user.getPasswordDesensitized());
    }

    @Test
    public void testAddressDesensitization() throws JsonProcessingException {
        UserInfo user = new UserInfo();
        user.setAddress("北京市朝阳区建国门外大街1号");

        String json = objectMapper.writeValueAsString(user);
        System.out.println("地址脱敏测试: " + json);

        // 验证地址脱敏
        assertEquals("北京市****************", user.getAddressDesensitized());
    }

    @Test
    public void testCustomDesensitization() throws JsonProcessingException {
        UserInfo user = new UserInfo();
        user.setCustomField("1234567890");

        String json = objectMapper.writeValueAsString(user);
        System.out.println("自定义脱敏测试: " + json);

        // 验证自定义脱敏：保留前3位和后3位
        assertEquals("123****890", user.getCustomFieldDesensitized());
    }

    /**
     * 测试用户信息类
     */
    public static class UserInfo {
        @Desensitize(type = DesensitizeType.USERNAME)
        private String username;

        @Desensitize(type = DesensitizeType.PHONE)
        private String phone;

        @Desensitize(type = DesensitizeType.EMAIL)
        private String email;

        @Desensitize(type = DesensitizeType.ID_CARD)
        private String idCard;

        @Desensitize(type = DesensitizeType.BANK_CARD)
        private String bankCard;

        @Desensitize(type = DesensitizeType.PASSWORD)
        private String password;

        @Desensitize(type = DesensitizeType.ADDRESS)
        private String address;

        @Desensitize(type = DesensitizeType.CUSTOM, prefixKeep = 3, suffixKeep = 3)
        private String customField;

        // 为了测试方便，提供手动脱敏的方法
        public String getUsernameDesensitized() {
            return DesensitizationUtils.desensitize(username, DesensitizeType.USERNAME, 0, 0, '*');
        }

        public String getPhoneDesensitized() {
            return DesensitizationUtils.desensitize(phone, DesensitizeType.PHONE, 0, 0, '*');
        }

        public String getEmailDesensitized() {
            return DesensitizationUtils.desensitize(email, DesensitizeType.EMAIL, 0, 0, '*');
        }

        public String getIdCardDesensitized() {
            return DesensitizationUtils.desensitize(idCard, DesensitizeType.ID_CARD, 0, 0, '*');
        }

        public String getBankCardDesensitized() {
            return DesensitizationUtils.desensitize(bankCard, DesensitizeType.BANK_CARD, 0, 0, '*');
        }

        public String getPasswordDesensitized() {
            return DesensitizationUtils.desensitize(password, DesensitizeType.PASSWORD, 0, 0, '*');
        }

        public String getAddressDesensitized() {
            return DesensitizationUtils.desensitize(address, DesensitizeType.ADDRESS, 0, 0, '*');
        }

        public String getCustomFieldDesensitized() {
            return DesensitizationUtils.desensitize(customField, DesensitizeType.CUSTOM, 3, 3, '*');
        }

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getIdCard() {
            return idCard;
        }

        public void setIdCard(String idCard) {
            this.idCard = idCard;
        }

        public String getBankCard() {
            return bankCard;
        }

        public void setBankCard(String bankCard) {
            this.bankCard = bankCard;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCustomField() {
            return customField;
        }

        public void setCustomField(String customField) {
            this.customField = customField;
        }
    }
}