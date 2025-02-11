/*
 * Copyright (c) 2010, 2024 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
module.exports = {
  rules: {
    'indent': ['warn', 2, {'SwitchCase': 1}],
    'no-extra-parens': ['off'],
    'accessor-pairs': 'warn',
    'array-callback-return': 'error',
    'guard-for-in': 'off',
    'no-alert': 'warn',
    'no-eval': 'error',
    'no-extra-bind': 'error',
    'no-extra-label': 'error',
    'no-implicit-coercion': 'off',
    'no-implied-eval': 'error',
    'no-invalid-this': 'off',
    'no-iterator': 'error',
    'no-caller': 'warn',
    'no-console': 'off',
    'no-labels': 'error',
    'no-undef': 'off',
    'no-throw-literal': 'off',
    'camelcase': ['error', {allow: ['^\\$', '_']}],
    'no-lone-blocks': 'error',
    'no-loop-func': 'error',
    'no-multi-spaces': 'warn',
    'no-proto': 'error',
    'no-prototype-builtins': 'off',
    'no-return-assign': 'error',
    'eslint func-call-spacing': 'off',
    'no-return-await': 'off',
    'no-self-compare': 'error',
    'no-label-var': 'error',
    'no-shadow': 'off',
    'no-var': 'warn',
    'eol-last': ['warn', 'always'],
    'comma-spacing': ['error', {'before': false, 'after': true}],
    'array-bracket-spacing': ['warn', 'never'],
    'brace-style': ['error', '1tbs'],
    'require-unicode-regexp': 'off',
    'no-sequences': 'error',
    'no-unmodified-loop-condition': 'error',
    'computed-property-spacing': ['warn', 'never'],
    'no-useless-return': 'error',
    'no-else-return': 'error',
    'max-len': ['warn', 240, 2, {ignoreUrls: true, ignorePattern: '^import .*'}],
    'semi': ['error', 'always'],
    'quotes': ['warn', 'single'],
    'comma-dangle': ['error', 'never'],
    'object-curly-spacing': ['error', 'never'],
    'operator-linebreak': 'off',
    'arrow-parens': ['error', 'as-needed'],
    'arrow-spacing': 'warn',
    'no-duplicate-imports': 'error',
    'one-var': 'off',
    'padded-blocks': 'off',
    'prefer-arrow-callback': 'warn',
    'prefer-template': 'off',
    'template-curly-spacing': ['error', 'never'],
    'linebreak-style': ['error', 'unix'],
    'newline-per-chained-call': 'off',
    'no-lonely-if': 'off',
    'new-cap': ['error', {'capIsNewExceptions': ['Deferred', '$.Event'], 'capIsNewExceptionPattern': 'Model$'}],
    'no-multi-assign': 'error',
    'no-unused-vars': 'off',
    'require-jsdoc': 'off',
    'no-plusplus': 'off',
    'no-trailing-spaces': 'warn',
    'space-before-function-paren': ['warn', {
      'anonymous': 'never',
      'named': 'never',
      'asyncArrow': 'always'
    }],
    'curly': ['error', 'all'],
    'eqeqeq': ['error', 'always', {'null': 'ignore'}],
    'spaced-comment': ['warn', 'always', {'exceptions': ['*']}],
    'valid-jsdoc': 'off',
    'prefer-const': 'off',
    'prefer-rest-params': 'warn',
    'prefer-spread': 'warn',
    'max-classes-per-file': 'off'
  }
};
