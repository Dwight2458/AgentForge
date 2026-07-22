import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useWorkspaceStore } from './workspace'

describe('workspace store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('records Grill responses and cancellation state', () => {
    const store = useWorkspaceStore()
    const originalCount = store.messages.length

    store.addMessage('Keep existing pagination behavior.')
    store.cancelRun()

    expect(store.messages).toHaveLength(originalCount + 1)
    expect(store.messages.at(-1)?.content).toBe('Keep existing pagination behavior.')
    expect(store.runCancelled).toBe(true)
  })
})

