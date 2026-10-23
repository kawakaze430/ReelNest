#!/usr/bin/env python3
"""Offline lexical sanity for .tf only; not a substitute for terraform fmt/validate."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'infrastructure/aws'
files=list(ROOT.glob('*.tf'))
assert files,'Missing Terraform resources'
for file in files:
    s=file.read_text()
    stack=[];quoted=False;comment=False;escape=False
    for i,c in enumerate(s):
        if comment:
            if c=='\n':comment=False
            continue
        if quoted:
            if escape:escape=False
            elif c=='\\':escape=True
            elif c=='"':quoted=False
            continue
        if c=='"':quoted=True;continue
        if c=='#' or (c=='/' and i+1<len(s) and s[i+1]=='/'):
            comment=True;continue
        if c==';':raise AssertionError(f'Unsupported HCL semicolon separator in {file}:{i}')
        if c in '{[(':
            stack.append((c,i));continue
        if c in '}])':
            assert stack, f'Unexpected {c} in {file}:{i}'
            opener,loc=stack.pop()
            assert opener+c in ('{}','[]','()'), f'Mismatch {opener} at {loc} / {c} at {i} in {file}'
    assert not quoted,f'Unclosed string in {file}'
    assert not stack,f'Unclosed HCL grouping in {file}: {stack}'
print(f'PASS: {len(files)} .tf files lexically balanced, no semicolon separators; provider validation still required')
