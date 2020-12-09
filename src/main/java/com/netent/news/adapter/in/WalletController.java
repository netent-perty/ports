package com.netent.news.adapter.in;

import com.netent.news.domain.WalletId;
import com.netent.news.application.port.in.GetBalanceResponse;
import com.netent.news.application.port.in.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{wallet}/balance")
    GetBalanceResponse getBalance(@PathVariable WalletId wallet) {
        return walletService.getBalance(wallet);
    }
}
