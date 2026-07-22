import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StageRail from './StageRail.vue'

describe('StageRail', () => {
  it('renders all stages and marks the active stage', () => {
    const wrapper = mount(StageRail, { props: { current: 3 } })

    expect(wrapper.findAll('li')).toHaveLength(5)
    expect(wrapper.findAll('li')[0]?.classes()).toContain('complete')
    expect(wrapper.findAll('li')[2]?.classes()).toContain('active')
    expect(wrapper.text()).toContain('Deliver')
  })
})

