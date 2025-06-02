name: "🚀 Pull Request"
description: "기능 추가, 버그 수정 등 PR 생성 시 필수 정보를 입력하세요"
labels: ["needs-review"]
body:
  - type: input
    id: title
    attributes:
      label: "🔖 PR 제목"
      description: "변경 사항을 한 줄로 요약해 주세요"
      placeholder: "[Feature] 로그인 API 추가"
    validations:
      required: true

  - type: textarea
    id: description
    attributes:
      label: "📄 상세 설명"
      description: "무엇을, 왜, 어떻게 바꿨는지 작성해 주세요"
      placeholder: |
        - 변경 내용  
        - 구현 방식  
        - 검증 방법  
    validations:
      required: true

  - type: textarea
    id: checklist
    attributes:
      label: "✅ 체크리스트"
      description: "작업 완료 사항을 체크박스로 작성해 주세요"
      placeholder: |
        - [ ] 기능 구현 완료  
        - [ ] 단위 테스트 작성  
        - [ ] 문서(README) 업데이트  
    validations:
      required: true

  - type: textarea
    id: related_issues
    attributes:
      label: "🔗 관련 이슈"
      description: "연관된 이슈 번호를 적어주세요 (예: #123, #456)"
      placeholder: "#123, #456"
    validations:
      required: false
