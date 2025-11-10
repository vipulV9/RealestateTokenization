package com.product.RealestateTokenization.model;


import com.product.RealestateTokenization.util.CryptoUtil;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;




@Entity
@Data
public class Wallet {
    @Id @GeneratedValue private Long id;
    private Long userId;
    private String address, secretEncrypted;
    private boolean fundedXrp = false;

    public void setUser(User user) { this.userId = user.getId(); }


}

