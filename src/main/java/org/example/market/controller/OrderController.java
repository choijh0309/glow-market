package org.example.market.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.market.controller.dto.BuyProductRequest;
import org.example.market.controller.dto.OrderCompleteResponse;
import org.example.market.domain.Member;
import org.example.market.domain.Orders;
import org.example.market.domain.Product;
import org.example.market.exception.InsufficientStockException;
import org.example.market.exception.OrderNotFoundException;
import org.example.market.exception.ProductNotFoundException;
import org.example.market.exception.UnauthorizedException;
import org.example.market.repository.OrderRepository;
import org.example.market.service.MemberService;
import org.example.market.service.OrderService;
import org.example.market.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final ProductService productService;
    private final MemberService memberService;
    private final OrderService orderService;

    @PostMapping("/{id}/reserve") // 예약
    public ResponseEntity<?> reserveProduct(@PathVariable Long id, @RequestBody BuyProductRequest buyProductRequest, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        Product product=productService.findById(id).orElseThrow(() -> new ProductNotFoundException("존재하지 않는 상품입니다."));
        if(product.getStock()< buyProductRequest.getQuantity()){
            throw new InsufficientStockException("재고가 부족합니다");
        }
        Member buyer = memberService.findByUsername(userDetails.getUsername()).orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));
        orderService.reserveProduct(id,buyer,buyProductRequest.getPrice(), buyProductRequest.getQuantity());
        return ResponseEntity.status(HttpStatus.CONFLICT).body("구매할 수 없는 제품입니다.");
    }

    @PostMapping("/{id}/approve") // 판매승인
    public ResponseEntity<?> approveSale(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        Member seller = memberService.findByUsername(userDetails.getUsername()).orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));
        Orders order = orderService.findById(id);
        orderService.approveSale(order,seller);

        return ResponseEntity.ok(new OrderCompleteResponse(id, order.getBuyer().getId(), order.getQuantity(), order.getStatus()));
    }
}
