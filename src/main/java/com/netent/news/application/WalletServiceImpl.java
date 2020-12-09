package com.netent.news.application;

import com.netent.news.domain.WalletId;
import com.netent.news.application.port.in.GetBalanceResponse;
import com.netent.news.application.port.in.WalletService;
import com.netent.news.application.port.out.OperatorGetBalance;
import com.netent.news.application.port.out.OperatorWalletFacade;
import org.springframework.stereotype.Component;

@Component
public class
WalletServiceImpl implements WalletService {
    private final OperatorWalletFacade operatorWalletFacade;

    public WalletServiceImpl(OperatorWalletFacade operatorWalletFacade) {
        this.operatorWalletFacade = operatorWalletFacade;
    }

    @Override
    public GetBalanceResponse getBalance(WalletId wallet) {
        OperatorGetBalance balance = operatorWalletFacade.getBalance(wallet);
        return new GetBalanceResponse();
    }
}
