package com.netent.news.port.in;

import com.netent.news.domain.WalletId;
import org.springframework.stereotype.Service;

@Service
public interface WalletService {
    GetBalanceResponse getBalance(WalletId wallet);
}
