package com.example.demo.iface.dto.res;

import com.example.demo.application.shared.dto.OrderQueriedView;

public record OrderQueriedResource(String code, String message, OrderQueriedView data) {

}
