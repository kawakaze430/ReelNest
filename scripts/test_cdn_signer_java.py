#!/usr/bin/env python3
"""Compile/execute real CloudFront signer with tiny HTTP/Spring stubs (no network/Maven).
Checks its signed policy against a generated RSA public key, rather than merely scanning source.
"""
import subprocess
import tempfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix='reelnest-signing-') as td:
    work=Path(td)
    stubs={
      'jakarta/servlet/http/HttpServletResponse.java':'''package jakarta.servlet.http;
public interface HttpServletResponse { void addHeader(String name,String value); }''',
      'org/springframework/beans/factory/annotation/Value.java':'''package org.springframework.beans.factory.annotation;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.PARAMETER,ElementType.FIELD})
public @interface Value { String value(); }''',
      'org/springframework/stereotype/Component.java':'''package org.springframework.stereotype;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
public @interface Component {}''',
      'org/springframework/http/HttpHeaders.java':'''package org.springframework.http;
public class HttpHeaders { public static final String SET_COOKIE="Set-Cookie"; }''',
      'org/springframework/http/HttpStatus.java':'''package org.springframework.http;
public enum HttpStatus { SERVICE_UNAVAILABLE, FORBIDDEN; }''',
      'org/springframework/web/server/ResponseStatusException.java':'''package org.springframework.web.server;
public class ResponseStatusException extends RuntimeException {
  public ResponseStatusException(Object status,String text){ super(text); }
  public ResponseStatusException(Object status,String text,Throwable t){super(text,t);}
}'''
    }
    for p,content in stubs.items():
        f=work/p;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(content)
    harness=work/'TestSigner.java'
    harness.write_text(r'''import com.reelnest.playback.CdnCookieSigner;
import jakarta.servlet.http.HttpServletResponse;
import java.security.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
public class TestSigner {
 static class Response implements HttpServletResponse {
   Map<String,String> cookies=new HashMap<>();
   public void addHeader(String h,String v) {
     if(!"Set-Cookie".equals(h))throw new AssertionError("Bad header");
     String pair=v.substring(0,v.indexOf(';'));cookies.put(pair.substring(0,pair.indexOf('=')),v);
     if(!v.contains("Secure")||!v.contains("HttpOnly")||!v.contains("Domain=.example.com"))throw new AssertionError("Unsafe cookies");
   }
 }
 static String unencode(String s) { return s.replace('-','+').replace('~','/').replace('_','='); }
 static String value(String cookie) {String p=cookie.substring(0,cookie.indexOf(';'));return p.substring(p.indexOf('=')+1);}
 public static void main(String[] args) throws Exception {
   KeyPairGenerator g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);
   KeyPair keys=g.generateKeyPair();
   String pem="-----BEGIN PRIVATE KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(keys.getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----";
   CdnCookieSigner signer=new CdnCookieSigner("https://stream.example.com",".example.com","PUBLICKEY01",pem);
   Response r=new Response();
   signer.grant(r,"processed/asset/job/attempt-1/master.m3u8");
   if(r.cookies.size()!=3)throw new AssertionError("Missing signed cookies");
   String policy=new String(Base64.getDecoder().decode(unencode(value(r.cookies.get("CloudFront-Policy")))),StandardCharsets.UTF_8);
   if(!policy.contains("https://stream.example.com/processed/asset/job/attempt-1/*"))throw new AssertionError("Scope incorrect: "+policy);
   String sig=value(r.cookies.get("CloudFront-Signature"));
   Signature verify=Signature.getInstance("SHA1withRSA");verify.initVerify(keys.getPublic());verify.update(policy.getBytes(StandardCharsets.UTF_8));
   if(!verify.verify(Base64.getDecoder().decode(unencode(sig))))throw new AssertionError("Signature invalid");
   if(!value(r.cookies.get("CloudFront-Key-Pair-Id")).equals("PUBLICKEY01"))throw new AssertionError("Wrong public key id");
   try {signer.grant(new Response(),"../../master.m3u8");throw new AssertionError("Traversal accepted");}
   catch(org.springframework.web.server.ResponseStatusException good){}
   System.out.println("PASS: signed CloudFront policy cryptographically verifies; cookies scoped and guarded");
 }
}
''')
    signer=root/'apps/api/src/main/java/com/reelnest/playback/CdnCookieSigner.java'
    java_files=[str(work/p) for p in stubs]+[str(signer),str(harness)]
    subprocess.run(['javac','-d',str(work/'classes'),*java_files],check=True)
    subprocess.run(['java','-cp',str(work/'classes'),'TestSigner'],check=True)
