import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useOutletContext } from 'react-router-dom';
import {
  createCiUserMapping,
  createGeo,
  createConfigurationItem,
  createGeoShiftMapping,
  createShift,
  createTeamMember,
  deleteCiUserMapping,
  deleteConfigurationItem,
  deleteGeo,
  deleteGeoShiftMapping,
  deleteShift,
  deleteTeamMember,
  fetchCiUserMappings,
  fetchConfigurationItems,
  fetchGeos,
  fetchGeoShiftMappings,
  fetchShifts,
  fetchTeamMembers,
  updateCiUserMapping,
  updateGeo,
  updateConfigurationItem,
  updateGeoShiftMapping,
  updateShift,
  updateTeamMember,
} from '../services/admin';
import { fetchSetupStatus } from '../services/setup';
import { fetchServiceNowConfig, updateServiceNowConfig } from '../services/servicenow';
import './SetupPage.css';

const INLINE_STEP_KEYS = new Set([
  'geos',
  'shifts',
  'configuration_items',
  'team_members',
  'geo_shift_mappings',
  'ci_user_mappings',
]);

export default function SetupPage() {
  const navigate = useNavigate();
  const outletContext = useOutletContext() || {};
  const [status, setStatus] = useState(null);
  const [serviceNowConfig, setServiceNowConfig] = useState(null);
  const [geos, setGeos] = useState([]);
  const [shifts, setShifts] = useState([]);
  const [configurationItems, setConfigurationItems] = useState([]);
  const [teamMembers, setTeamMembers] = useState([]);
  const [geoShiftMappings, setGeoShiftMappings] = useState([]);
  const [ciUserMappings, setCiUserMappings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [currentStepKey, setCurrentStepKey] = useState('');

  const [geoName, setGeoName] = useState('');
  const [editingGeoId, setEditingGeoId] = useState(null);
  const [geoError, setGeoError] = useState('');
  const [geoSaving, setGeoSaving] = useState(false);

  const [shiftName, setShiftName] = useState('');
  const [editingShiftId, setEditingShiftId] = useState(null);
  const [shiftError, setShiftError] = useState('');
  const [shiftSaving, setShiftSaving] = useState(false);

  const [ciName, setCiName] = useState('');
  const [ciDescription, setCiDescription] = useState('');
  const [ciSysId, setCiSysId] = useState('');
  const [editingCiId, setEditingCiId] = useState(null);
  const [ciError, setCiError] = useState('');
  const [ciSaving, setCiSaving] = useState(false);

  const [teamFirstName, setTeamFirstName] = useState('');
  const [teamLastName, setTeamLastName] = useState('');
  const [teamEmail, setTeamEmail] = useState('');
  const [teamPhone, setTeamPhone] = useState('');
  const [teamSysId, setTeamSysId] = useState('');
  const [teamGeoId, setTeamGeoId] = useState('');
  const [editingTeamMemberId, setEditingTeamMemberId] = useState(null);
  const [teamMemberError, setTeamMemberError] = useState('');
  const [teamMemberSaving, setTeamMemberSaving] = useState(false);

  const [mappingGeoId, setMappingGeoId] = useState('');
  const [mappingShiftId, setMappingShiftId] = useState('');
  const [editingGeoShiftMappingId, setEditingGeoShiftMappingId] = useState(null);
  const [geoShiftError, setGeoShiftError] = useState('');
  const [geoShiftSaving, setGeoShiftSaving] = useState(false);

  const [mappingConfigurationItemId, setMappingConfigurationItemId] = useState('');
  const [mappingTeamMemberId, setMappingTeamMemberId] = useState('');
  const [mappingSortOrder, setMappingSortOrder] = useState('');
  const [editingCiUserMappingId, setEditingCiUserMappingId] = useState(null);
  const [ciUserError, setCiUserError] = useState('');
  const [ciUserSaving, setCiUserSaving] = useState(false);

  const [serviceNowInstanceUrl, setServiceNowInstanceUrl] = useState('');
  const [serviceNowUsername, setServiceNowUsername] = useState('');
  const [serviceNowPassword, setServiceNowPassword] = useState('');
  const [serviceNowError, setServiceNowError] = useState('');
  const [serviceNowSaving, setServiceNowSaving] = useState(false);

  const canAccessStep = useCallback((steps, index) => {
    if (index <= 0) {
      return true;
    }
    return steps.slice(0, index).every((step) => step.complete);
  }, []);

  const resolveStepKey = useCallback((steps, preferredStepKey) => {
    const accessibleStepKeys = steps
      .filter((step, index) => canAccessStep(steps, index))
      .map((step) => step.key);
    if (preferredStepKey && accessibleStepKeys.includes(preferredStepKey)) {
      return preferredStepKey;
    }
    const nextPendingStep = steps.find((step, index) => !step.complete && canAccessStep(steps, index));
    return nextPendingStep?.key || steps[0]?.key || '';
  }, [canAccessStep]);

  const loadSetupData = useCallback(async (preferredStepKey = null) => {
    setLoading(true);
    setError('');
    try {
      const [setupData, configData, geoData, shiftData, ciData, teamMemberData, geoShiftData, ciUserData] = await Promise.all([
        fetchSetupStatus(),
        fetchServiceNowConfig(),
        fetchGeos(),
        fetchShifts(),
        fetchConfigurationItems(),
        fetchTeamMembers(),
        fetchGeoShiftMappings(),
        fetchCiUserMappings(),
      ]);
      setStatus(setupData);
      setServiceNowConfig(configData);
      setServiceNowInstanceUrl(configData?.instanceUrl || '');
      setServiceNowUsername(configData?.username || '');
      setServiceNowPassword('');
      setGeos(geoData);
      setShifts(shiftData);
      setConfigurationItems(ciData);
      setTeamMembers(teamMemberData);
      setGeoShiftMappings(geoShiftData);
      setCiUserMappings(ciUserData);
      setCurrentStepKey((previousStepKey) =>
        resolveStepKey(setupData.steps || [], preferredStepKey || previousStepKey)
      );
    } catch (err) {
      setError('Failed to load setup status.');
    } finally {
      setLoading(false);
    }
  }, [resolveStepKey]);

  useEffect(() => {
    loadSetupData();
  }, [loadSetupData]);

  async function handleServiceNowSubmit(event) {
    event.preventDefault();
    setServiceNowError('');
    setServiceNowSaving(true);
    try {
      await updateServiceNowConfig({
        instanceUrl: serviceNowInstanceUrl,
        username: serviceNowUsername,
        password: serviceNowPassword,
      });
      setServiceNowPassword('');
      await loadSetupData('geos');
    } catch (err) {
      setServiceNowError(
        typeof err?.response?.data === 'string'
          ? err.response.data
          : 'Failed to connect ServiceNow.'
      );
    } finally {
      setServiceNowSaving(false);
    }
  }

  function resetGeoForm() {
    setGeoName('');
    setEditingGeoId(null);
    setGeoError('');
  }

  function resetShiftForm() {
    setShiftName('');
    setEditingShiftId(null);
    setShiftError('');
  }

  function resetConfigurationItemForm() {
    setCiName('');
    setCiDescription('');
    setCiSysId('');
    setEditingCiId(null);
    setCiError('');
  }

  function resetTeamMemberForm() {
    setTeamFirstName('');
    setTeamLastName('');
    setTeamEmail('');
    setTeamPhone('');
    setTeamSysId('');
    setTeamGeoId('');
    setEditingTeamMemberId(null);
    setTeamMemberError('');
  }

  function resetGeoShiftForm() {
    setMappingGeoId('');
    setMappingShiftId('');
    setEditingGeoShiftMappingId(null);
    setGeoShiftError('');
  }

  function resetCiUserForm() {
    setMappingConfigurationItemId('');
    setMappingTeamMemberId('');
    setMappingSortOrder('');
    setEditingCiUserMappingId(null);
    setCiUserError('');
  }

  async function handleGeoSubmit(event) {
    event.preventDefault();
    setGeoError('');
    if (!geoName.trim()) {
      setGeoError('Geo name is required.');
      return;
    }

    setGeoSaving(true);
    try {
      if (editingGeoId) {
        await updateGeo(editingGeoId, { name: geoName.trim() });
      } else {
        await createGeo({ name: geoName.trim() });
      }
      resetGeoForm();
      await loadSetupData('geos');
    } catch (err) {
      setGeoError('Failed to save geo.');
    } finally {
      setGeoSaving(false);
    }
  }

  async function handleGeoDelete(id) {
    if (!window.confirm('Delete this geo?')) {
      return;
    }
    setGeoError('');
    try {
      await deleteGeo(id);
      if (editingGeoId === id) {
        resetGeoForm();
      }
      await loadSetupData('geos');
    } catch (err) {
      setGeoError('Failed to delete geo.');
    }
  }

  async function handleShiftSubmit(event) {
    event.preventDefault();
    setShiftError('');
    if (!shiftName.trim()) {
      setShiftError('Shift name is required.');
      return;
    }

    setShiftSaving(true);
    try {
      if (editingShiftId) {
        await updateShift(editingShiftId, { name: shiftName.trim() });
      } else {
        await createShift({ name: shiftName.trim() });
      }
      resetShiftForm();
      await loadSetupData('shifts');
    } catch (err) {
      setShiftError('Failed to save shift.');
    } finally {
      setShiftSaving(false);
    }
  }

  async function handleShiftDelete(id) {
    if (!window.confirm('Delete this shift?')) {
      return;
    }
    setShiftError('');
    try {
      await deleteShift(id);
      if (editingShiftId === id) {
        resetShiftForm();
      }
      await loadSetupData('shifts');
    } catch (err) {
      setShiftError('Failed to delete shift.');
    }
  }

  async function handleConfigurationItemSubmit(event) {
    event.preventDefault();
    setCiError('');
    if (!ciName.trim()) {
      setCiError('Configuration item name is required.');
      return;
    }
    if (!ciSysId.trim()) {
      setCiError('ServiceNow CI Sys ID is required.');
      return;
    }

    setCiSaving(true);
    try {
      const payload = {
        name: ciName.trim(),
        description: ciDescription.trim(),
        serviceNowSysId: ciSysId.trim(),
      };
      if (editingCiId) {
        await updateConfigurationItem(editingCiId, payload);
      } else {
        await createConfigurationItem(payload);
      }
      resetConfigurationItemForm();
      await loadSetupData('configuration_items');
    } catch (err) {
      setCiError('Failed to save configuration item.');
    } finally {
      setCiSaving(false);
    }
  }

  async function handleConfigurationItemDelete(id) {
    if (!window.confirm('Delete this configuration item?')) {
      return;
    }
    setCiError('');
    try {
      await deleteConfigurationItem(id);
      if (editingCiId === id) {
        resetConfigurationItemForm();
      }
      await loadSetupData('configuration_items');
    } catch (err) {
      setCiError('Failed to delete configuration item.');
    }
  }

  async function handleTeamMemberSubmit(event) {
    event.preventDefault();
    setTeamMemberError('');
    if (!teamFirstName.trim() || !teamLastName.trim()) {
      setTeamMemberError('First name and last name are required.');
      return;
    }
    if (!teamEmail.trim()) {
      setTeamMemberError('Email is required.');
      return;
    }
    if (!teamGeoId) {
      setTeamMemberError('Geo is required.');
      return;
    }
    if (!teamSysId.trim()) {
      setTeamMemberError('ServiceNow User Sys ID is required.');
      return;
    }

    setTeamMemberSaving(true);
    try {
      const payload = {
        f_name: teamFirstName.trim(),
        l_name: teamLastName.trim(),
        email: teamEmail.trim(),
        phone: teamPhone.trim(),
        sys_id: teamSysId.trim(),
        geoId: Number(teamGeoId),
      };
      if (editingTeamMemberId) {
        await updateTeamMember(editingTeamMemberId, payload);
      } else {
        await createTeamMember(payload);
      }
      resetTeamMemberForm();
      await loadSetupData('team_members');
    } catch (err) {
      setTeamMemberError('Failed to save team member.');
    } finally {
      setTeamMemberSaving(false);
    }
  }

  async function handleTeamMemberDelete(id) {
    if (!window.confirm('Delete this team member?')) {
      return;
    }
    setTeamMemberError('');
    try {
      await deleteTeamMember(id);
      if (editingTeamMemberId === id) {
        resetTeamMemberForm();
      }
      await loadSetupData('team_members');
    } catch (err) {
      setTeamMemberError('Failed to delete team member.');
    }
  }

  async function handleGeoShiftSubmit(event) {
    event.preventDefault();
    setGeoShiftError('');
    if (!mappingGeoId || !mappingShiftId) {
      setGeoShiftError('Geo and shift are required.');
      return;
    }

    setGeoShiftSaving(true);
    try {
      const payload = { geoId: Number(mappingGeoId), shiftId: Number(mappingShiftId) };
      if (editingGeoShiftMappingId) {
        await updateGeoShiftMapping(editingGeoShiftMappingId, payload);
      } else {
        await createGeoShiftMapping(payload);
      }
      resetGeoShiftForm();
      await loadSetupData('geo_shift_mappings');
    } catch (err) {
      setGeoShiftError('Failed to save geo-shift mapping.');
    } finally {
      setGeoShiftSaving(false);
    }
  }

  async function handleGeoShiftDelete(id) {
    if (!window.confirm('Delete this geo-shift mapping?')) {
      return;
    }
    setGeoShiftError('');
    try {
      await deleteGeoShiftMapping(id);
      if (editingGeoShiftMappingId === id) {
        resetGeoShiftForm();
      }
      await loadSetupData('geo_shift_mappings');
    } catch (err) {
      setGeoShiftError('Failed to delete geo-shift mapping.');
    }
  }

  async function handleCiUserSubmit(event) {
    event.preventDefault();
    setCiUserError('');
    if (!mappingConfigurationItemId || !mappingTeamMemberId) {
      setCiUserError('Configuration item and team member are required.');
      return;
    }

    setCiUserSaving(true);
    try {
      const payload = {
        configurationItemId: Number(mappingConfigurationItemId),
        teamMemberId: Number(mappingTeamMemberId),
        sortOrder: mappingSortOrder === '' ? null : Number(mappingSortOrder),
      };
      if (editingCiUserMappingId) {
        await updateCiUserMapping(editingCiUserMappingId, payload);
      } else {
        await createCiUserMapping(payload);
      }
      resetCiUserForm();
      await loadSetupData('ci_user_mappings');
    } catch (err) {
      setCiUserError('Failed to save CI-user mapping.');
    } finally {
      setCiUserSaving(false);
    }
  }

  async function handleCiUserDelete(id) {
    if (!window.confirm('Delete this CI-user mapping?')) {
      return;
    }
    setCiUserError('');
    try {
      await deleteCiUserMapping(id);
      if (editingCiUserMappingId === id) {
        resetCiUserForm();
      }
      await loadSetupData('ci_user_mappings');
    } catch (err) {
      setCiUserError('Failed to delete CI-user mapping.');
    }
  }

  if (loading) {
    return (
      <div className="container py-3">
        <div className="text-center my-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container py-3">
        <div className="alert alert-danger">{error}</div>
      </div>
    );
  }

  const steps = status?.steps || [];
  const requiredSteps = steps.filter((step) => step.required);
  const optionalSteps = steps.filter((step) => !step.required);
  const ready = Boolean(status?.ready);
  const teamName = outletContext.currentUser?.workspace?.teamName || 'Current Team';
  const organizationName = outletContext.currentUser?.workspace?.organizationName || 'Your Organization';
  const currentStepIndex = Math.max(steps.findIndex((step) => step.key === currentStepKey), 0);
  const currentStep = steps[currentStepIndex];
  const previousStep = currentStepIndex > 0 ? steps[currentStepIndex - 1] : null;
  const nextStep = currentStepIndex < steps.length - 1 ? steps[currentStepIndex + 1] : null;
  const currentStepAccessible = currentStep ? canAccessStep(steps, currentStepIndex) : true;
  const nextAccessibleStep = steps.find((step, index) => canAccessStep(steps, index) && !step.complete);
  const completionPercent = status?.totalSteps
    ? Math.round(((status?.completedSteps || 0) / status.totalSteps) * 100)
    : 0;

  return (
    <div className="container py-3 setup-page">
      <div className="setup-hero mb-4">
        <h2 className="mb-2">Team Setup: {teamName}</h2>
        <div className="text-muted mb-2">
          Organization: {organizationName}
        </div>
        <p className="text-muted mb-2">
          Configure {teamName} step by step. Once these core items are in place,
          InciTeam is ready for scheduling and incident assignment for this team.
        </p>
        {status?.brandNew && (
          <div className="alert alert-primary">
            This looks like a brand-new team workspace. Complete the steps below to activate {teamName}.
          </div>
        )}
        <div className={`alert ${ready ? 'alert-success' : 'alert-info'} mb-0`}>
          {ready
            ? `${teamName} setup is complete and ready to launch.`
            : `${teamName} setup progress: ${status?.completedSteps || 0} of ${status?.totalSteps || 0} steps complete.`}
        </div>
        <div className="setup-hero__stats">
          <div className="setup-stat">
            <div className="setup-stat__label">Required Completion</div>
            <div className="setup-stat__value">{completionPercent}%</div>
            <div className="setup-progress mt-2">
              <div className="setup-progress__bar" style={{ width: `${completionPercent}%` }} />
            </div>
          </div>
          <div className="setup-stat">
            <div className="setup-stat__label">Required Steps</div>
            <div className="setup-stat__value">
              {status?.completedSteps || 0}/{status?.totalSteps || 0}
            </div>
          </div>
          <div className="setup-stat">
            <div className="setup-stat__label">Optional Tasks</div>
            <div className="setup-stat__value">{optionalSteps.filter((step) => !step.complete).length}</div>
          </div>
        </div>
      </div>

      {!ready && nextAccessibleStep && (
        <div className="card border-primary mb-4">
          <div className="card-body d-flex justify-content-between align-items-center gap-3 flex-wrap">
            <div>
              <h5 className="mb-1">Next Recommended Step: {nextAccessibleStep.label}</h5>
              <div className="text-muted">{nextAccessibleStep.description}</div>
            </div>
            <button
              className="btn btn-primary"
              onClick={() => setCurrentStepKey(nextAccessibleStep.key)}
              type="button"
            >
              Open Step
            </button>
          </div>
        </div>
      )}

      <div className="row g-4">
        <div className="col-12 col-xl-4">
          <div className="card h-100">
            <div className="card-body">
              <h5 className="card-title mb-3">Setup Steps</h5>
              <div className="list-group setup-step-nav">
                {steps.map((step, index) => (
                  <button
                    key={step.key}
                    type="button"
                    className={`list-group-item list-group-item-action d-flex justify-content-between align-items-start ${
                      step.key === currentStepKey ? 'active' : ''
                    }`}
                    onClick={() => canAccessStep(steps, index) && setCurrentStepKey(step.key)}
                    disabled={!canAccessStep(steps, index)}
                  >
                    <div className="me-3 text-start">
                      <div className="fw-semibold">
                        {index + 1}. {step.label}
                      </div>
                      <div className={`small ${step.key === currentStepKey ? 'text-white-50' : !canAccessStep(steps, index) ? 'text-warning' : 'text-muted'}`}>
                        {step.count} records
                      </div>
                    </div>
                    <span className={`badge ${
                      !step.required
                        ? 'bg-info text-dark'
                        : !canAccessStep(steps, index)
                          ? 'bg-warning text-dark'
                          : step.complete
                            ? 'bg-success'
                            : 'bg-secondary'
                    }`}>
                      {!step.required ? 'Optional' : !canAccessStep(steps, index) ? 'Locked' : step.complete ? 'Done' : 'Pending'}
                    </span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-8">
          {currentStep ? (
            <div className={`card setup-section-card border-${currentStep.complete ? 'success' : 'secondary'}`}>
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-3">
                  <div>
                    <h4 className="mb-1">{currentStep.label}</h4>
                    <div className="text-muted">{currentStep.description}</div>
                  </div>
                  <span className={`badge ${
                    !currentStep.required
                      ? (currentStep.complete ? 'bg-success' : 'bg-info text-dark')
                      : currentStep.complete
                        ? 'bg-success'
                        : 'bg-secondary'
                  }`}>
                    {!currentStep.required
                      ? (currentStep.complete ? 'Optional Complete' : 'Optional')
                      : currentStep.complete
                        ? 'Complete'
                        : 'Pending'}
                  </span>
                </div>

                {!currentStepAccessible && (
                  <div className="alert alert-warning">
                    Complete the earlier setup steps first to unlock this section.
                  </div>
                )}

                {currentStepAccessible && currentStep.key === 'servicenow_connection' && (
                  <InlineServiceNowConnectionStep
                    serviceNowConfig={serviceNowConfig}
                    serviceNowInstanceUrl={serviceNowInstanceUrl}
                    serviceNowUsername={serviceNowUsername}
                    serviceNowPassword={serviceNowPassword}
                    serviceNowError={serviceNowError}
                    serviceNowSaving={serviceNowSaving}
                    setServiceNowInstanceUrl={setServiceNowInstanceUrl}
                    setServiceNowUsername={setServiceNowUsername}
                    setServiceNowPassword={setServiceNowPassword}
                    onSubmit={handleServiceNowSubmit}
                  />
                )}

                {currentStepAccessible && currentStep.key === 'geos' && (
                  <InlineGeoStep
                    geos={geos}
                    geoName={geoName}
                    editingGeoId={editingGeoId}
                    geoError={geoError}
                    geoSaving={geoSaving}
                    setGeoName={setGeoName}
                    onSubmit={handleGeoSubmit}
                    onEdit={(geo) => {
                      setEditingGeoId(geo.g_id);
                      setGeoName(geo.name || '');
                      setGeoError('');
                    }}
                    onDelete={handleGeoDelete}
                    onCancel={resetGeoForm}
                  />
                )}

                {currentStepAccessible && currentStep.key === 'shifts' && (
                  <InlineShiftStep
                    shifts={shifts}
                    shiftName={shiftName}
                    editingShiftId={editingShiftId}
                    shiftError={shiftError}
                    shiftSaving={shiftSaving}
                    setShiftName={setShiftName}
                    onSubmit={handleShiftSubmit}
                    onEdit={(shift) => {
                      setEditingShiftId(shift.s_id);
                      setShiftName(shift.name || '');
                      setShiftError('');
                    }}
                    onDelete={handleShiftDelete}
                    onCancel={resetShiftForm}
                  />
                )}

                {currentStepAccessible && currentStep.key === 'configuration_items' && (
                  <InlineConfigurationItemStep
                    configurationItems={configurationItems}
                    ciName={ciName}
                    ciDescription={ciDescription}
                    ciSysId={ciSysId}
                    editingCiId={editingCiId}
                    ciError={ciError}
                    ciSaving={ciSaving}
                    setCiName={setCiName}
                    setCiDescription={setCiDescription}
                    setCiSysId={setCiSysId}
                    onSubmit={handleConfigurationItemSubmit}
                    onEdit={(item) => {
                      setEditingCiId(item.ci_id);
                      setCiName(item.name || '');
                      setCiDescription(item.description || '');
                      setCiSysId(item.serviceNowSysId || '');
                      setCiError('');
                    }}
                    onDelete={handleConfigurationItemDelete}
                    onCancel={resetConfigurationItemForm}
                  />
                )}

                {currentStepAccessible && currentStep.key === 'team_members' && (
                  <InlineTeamMemberStep
                    teamMembers={teamMembers}
                    geos={geos}
                    teamFirstName={teamFirstName}
                    teamLastName={teamLastName}
                    teamEmail={teamEmail}
                    teamPhone={teamPhone}
                    teamSysId={teamSysId}
                    teamGeoId={teamGeoId}
                    editingTeamMemberId={editingTeamMemberId}
                    teamMemberError={teamMemberError}
                    teamMemberSaving={teamMemberSaving}
                    setTeamFirstName={setTeamFirstName}
                    setTeamLastName={setTeamLastName}
                    setTeamEmail={setTeamEmail}
                    setTeamPhone={setTeamPhone}
                    setTeamSysId={setTeamSysId}
                    setTeamGeoId={setTeamGeoId}
                    onSubmit={handleTeamMemberSubmit}
                    onEdit={(member) => {
                      setEditingTeamMemberId(member.tm_id);
                      setTeamFirstName(member.f_name || '');
                      setTeamLastName(member.l_name || '');
                      setTeamEmail(member.email || '');
                      setTeamPhone(member.phone || '');
                      setTeamSysId(member.sys_id || '');
                      setTeamGeoId(member.geo?.g_id ? String(member.geo.g_id) : '');
                      setTeamMemberError('');
                    }}
                    onDelete={handleTeamMemberDelete}
                    onCancel={resetTeamMemberForm}
                  />
                )}

                {currentStepAccessible && currentStep.key === 'geo_shift_mappings' && (
                  <InlineGeoShiftMappingStep
                    geos={geos}
                    shifts={shifts}
                    geoShiftMappings={geoShiftMappings}
                    mappingGeoId={mappingGeoId}
                    mappingShiftId={mappingShiftId}
                    editingGeoShiftMappingId={editingGeoShiftMappingId}
                    geoShiftError={geoShiftError}
                    geoShiftSaving={geoShiftSaving}
                    setMappingGeoId={setMappingGeoId}
                    setMappingShiftId={setMappingShiftId}
                    onSubmit={handleGeoShiftSubmit}
                    onEdit={(mapping) => {
                      setEditingGeoShiftMappingId(mapping.gsm_id);
                      setMappingGeoId(mapping.geo?.g_id ? String(mapping.geo.g_id) : '');
                      setMappingShiftId(mapping.shift?.s_id ? String(mapping.shift.s_id) : '');
                      setGeoShiftError('');
                    }}
                    onDelete={handleGeoShiftDelete}
                    onCancel={resetGeoShiftForm}
                  />
                )}

                {currentStepAccessible && currentStep.key === 'ci_user_mappings' && (
                  <InlineCiUserMappingStep
                    configurationItems={configurationItems}
                    teamMembers={teamMembers}
                    ciUserMappings={ciUserMappings}
                    mappingConfigurationItemId={mappingConfigurationItemId}
                    mappingTeamMemberId={mappingTeamMemberId}
                    mappingSortOrder={mappingSortOrder}
                    editingCiUserMappingId={editingCiUserMappingId}
                    ciUserError={ciUserError}
                    ciUserSaving={ciUserSaving}
                    setMappingConfigurationItemId={setMappingConfigurationItemId}
                    setMappingTeamMemberId={setMappingTeamMemberId}
                    setMappingSortOrder={setMappingSortOrder}
                    onSubmit={handleCiUserSubmit}
                    onEdit={(mapping) => {
                      setEditingCiUserMappingId(mapping.mapping_id);
                      setMappingConfigurationItemId(mapping.configurationItem?.ci_id ? String(mapping.configurationItem.ci_id) : '');
                      setMappingTeamMemberId(mapping.teamMember?.tm_id ? String(mapping.teamMember.tm_id) : '');
                      setMappingSortOrder(mapping.sortOrder ?? '');
                      setCiUserError('');
                    }}
                    onDelete={handleCiUserDelete}
                    onCancel={resetCiUserForm}
                  />
                )}

                {currentStepAccessible && currentStep.key !== 'servicenow_connection' && !INLINE_STEP_KEYS.has(currentStep.key) && (
                  <GuidedExternalStep step={currentStep} onRefresh={() => loadSetupData(currentStep.key)} />
                )}

                <div className="d-flex justify-content-between align-items-center mt-4 gap-2 flex-wrap">
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    disabled={!previousStep}
                    onClick={() => previousStep && setCurrentStepKey(previousStep.key)}
                  >
                    Previous Step
                  </button>

                  <div className="d-flex gap-2 flex-wrap">
                    {!ready && nextAccessibleStep && nextAccessibleStep.key !== currentStep.key && (
                      <button
                        type="button"
                        className="btn btn-outline-primary"
                        onClick={() => nextAccessibleStep && setCurrentStepKey(nextAccessibleStep.key)}
                      >
                        Jump to Next Pending Step
                      </button>
                    )}
                    <button
                      type="button"
                      className="btn btn-primary"
                      disabled={!nextStep || !canAccessStep(steps, currentStepIndex + 1)}
                      onClick={() => nextStep && canAccessStep(steps, currentStepIndex + 1) && setCurrentStepKey(nextStep.key)}
                    >
                      Next Step
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ) : null}
        </div>
      </div>

      <div className="mt-4 d-flex gap-2">
        <Link className="btn btn-outline-secondary" to="/summary">
          Back to Summary
        </Link>
      </div>

      <div className="card mt-4 border-success setup-review-card">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-3">
            <div>
              <h4 className="mb-1">Review and Launch</h4>
              <div className="text-muted">
                Review {teamName} readiness before moving into daily operations.
              </div>
            </div>
            <span className={`badge ${ready ? 'bg-success' : 'bg-secondary'}`}>
              {ready ? `${teamName} Ready` : 'Setup In Progress'}
            </span>
          </div>

          <div className="row g-3 mb-3">
            <div className="col-12 col-lg-6">
              <div className="border rounded p-3 h-100">
                <h5 className="mb-2">Required Setup</h5>
                <div className="mb-2">
                  {status?.completedSteps || 0} of {status?.totalSteps || 0} required steps complete
                </div>
                <ul className="setup-review-list">
                  {requiredSteps.map((step) => (
                    <li key={step.key}>
                      {step.label}: {step.complete ? 'Complete' : 'Pending'}
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="col-12 col-lg-6">
              <div className="border rounded p-3 h-100">
                <h5 className="mb-2">Optional Follow-Up</h5>
                {optionalSteps.length > 0 ? (
                  <>
                    <div className="mb-2">These can be completed later without blocking launch.</div>
                    <ul className="setup-review-list">
                      {optionalSteps.map((step) => (
                        <li key={step.key}>
                          {step.label}: {step.complete ? 'Complete' : 'Recommended'}
                        </li>
                      ))}
                    </ul>
                  </>
                ) : (
                  <div className="mb-0">No optional follow-up steps yet.</div>
                )}
              </div>
            </div>
          </div>

          {!ready && (
            <div className="alert alert-warning mb-3">
              Complete all required setup steps before launching {teamName}.
            </div>
          )}

          {ready && optionalSteps.some((step) => !step.complete) && (
            <div className="alert alert-info mb-3">
              {teamName} is ready. You can launch now and come back later for optional setup like schedules.
            </div>
          )}

          {ready && (
            <div className="d-flex gap-2 flex-wrap">
              <button
                type="button"
                className="btn btn-success"
                onClick={() => navigate('/summary')}
              >
                Launch {teamName}
              </button>
              {optionalSteps.some((step) => step.key === 'schedules' && !step.complete) && (
                <Link className="btn btn-outline-primary" to="/schedules">
                  Add Schedules Now
                </Link>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function InlineGeoStep({
  geos,
  geoName,
  editingGeoId,
  geoError,
  geoSaving,
  setGeoName,
  onSubmit,
  onEdit,
  onDelete,
  onCancel,
}) {
  return (
    <>
      {geoError && <div className="alert alert-danger">{geoError}</div>}
      <form className="row g-3 mb-4" onSubmit={onSubmit}>
        <div className="col-md-8">
          <label className="form-label">Geo Name</label>
          <input
            type="text"
            className="form-control"
            value={geoName}
            onChange={(event) => setGeoName(event.target.value)}
            placeholder="e.g. APAC"
            required
          />
        </div>
        <div className="col-12 d-flex gap-2">
          <button type="submit" className="btn btn-primary" disabled={geoSaving}>
            {geoSaving ? 'Saving...' : editingGeoId ? 'Update Geo' : 'Add Geo'}
          </button>
          {editingGeoId && (
            <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <div className="table-responsive">
        <table className="table table-bordered align-middle mb-0 setup-inline-table">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th style={{ width: '180px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {geos.map((geo) => (
              <tr key={geo.g_id}>
                <td>{geo.g_id}</td>
                <td>{geo.name}</td>
                <td className="d-flex gap-2">
                  <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => onEdit(geo)}>
                    Edit
                  </button>
                  <button type="button" className="btn btn-outline-danger btn-sm" onClick={() => onDelete(geo.g_id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {geos.length === 0 && (
              <tr>
                <td colSpan={3} className="text-center">
                  No geos yet. Add your first supported region to continue.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function InlineShiftStep({
  shifts,
  shiftName,
  editingShiftId,
  shiftError,
  shiftSaving,
  setShiftName,
  onSubmit,
  onEdit,
  onDelete,
  onCancel,
}) {
  return (
    <>
      {shiftError && <div className="alert alert-danger">{shiftError}</div>}
      <form className="row g-3 mb-4" onSubmit={onSubmit}>
        <div className="col-md-8">
          <label className="form-label">Shift Name</label>
          <input
            type="text"
            className="form-control"
            value={shiftName}
            onChange={(event) => setShiftName(event.target.value)}
            placeholder="e.g. General"
            required
          />
        </div>
        <div className="col-12 d-flex gap-2">
          <button type="submit" className="btn btn-primary" disabled={shiftSaving}>
            {shiftSaving ? 'Saving...' : editingShiftId ? 'Update Shift' : 'Add Shift'}
          </button>
          {editingShiftId && (
            <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <div className="table-responsive">
        <table className="table table-bordered align-middle mb-0 setup-inline-table">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th style={{ width: '180px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {shifts.map((shift) => (
              <tr key={shift.s_id}>
                <td>{shift.s_id}</td>
                <td>{shift.name}</td>
                <td className="d-flex gap-2">
                  <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => onEdit(shift)}>
                    Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => onDelete(shift.s_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {shifts.length === 0 && (
              <tr>
                <td colSpan={3} className="text-center">
                  No shifts yet. Add your first shift to continue.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function InlineConfigurationItemStep({
  configurationItems,
  ciName,
  ciDescription,
  ciSysId,
  editingCiId,
  ciError,
  ciSaving,
  setCiName,
  setCiDescription,
  setCiSysId,
  onSubmit,
  onEdit,
  onDelete,
  onCancel,
}) {
  return (
    <>
      {ciError && <div className="alert alert-danger">{ciError}</div>}
      <form className="row g-3 mb-4" onSubmit={onSubmit}>
        <div className="col-md-4">
          <label className="form-label">Name</label>
          <input
            type="text"
            className="form-control"
            value={ciName}
            onChange={(event) => setCiName(event.target.value)}
            required
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">Description</label>
          <input
            type="text"
            className="form-control"
            value={ciDescription}
            onChange={(event) => setCiDescription(event.target.value)}
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">ServiceNow CI Sys ID</label>
          <input
            type="text"
            className="form-control"
            value={ciSysId}
            onChange={(event) => setCiSysId(event.target.value)}
            required
          />
        </div>
        <div className="col-12 d-flex gap-2">
          <button type="submit" className="btn btn-primary" disabled={ciSaving}>
            {ciSaving ? 'Saving...' : editingCiId ? 'Update CI' : 'Add CI'}
          </button>
          {editingCiId && (
            <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <div className="table-responsive">
        <table className="table table-bordered align-middle mb-0 setup-inline-table">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Description</th>
              <th>ServiceNow CI Sys ID</th>
              <th style={{ width: '180px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {configurationItems.map((item) => (
              <tr key={item.ci_id}>
                <td>{item.ci_id}</td>
                <td>{item.name}</td>
                <td>{item.description || '-'}</td>
                <td>{item.serviceNowSysId || '-'}</td>
                <td className="d-flex gap-2">
                  <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => onEdit(item)}>
                    Edit
                  </button>
                  <button type="button" className="btn btn-outline-danger btn-sm" onClick={() => onDelete(item.ci_id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {configurationItems.length === 0 && (
              <tr>
                <td colSpan={5} className="text-center">
                  No configuration items yet. Add your first CI to continue.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function InlineTeamMemberStep({
  teamMembers,
  geos,
  teamFirstName,
  teamLastName,
  teamEmail,
  teamPhone,
  teamSysId,
  teamGeoId,
  editingTeamMemberId,
  teamMemberError,
  teamMemberSaving,
  setTeamFirstName,
  setTeamLastName,
  setTeamEmail,
  setTeamPhone,
  setTeamSysId,
  setTeamGeoId,
  onSubmit,
  onEdit,
  onDelete,
  onCancel,
}) {
  return (
    <>
      {teamMemberError && <div className="alert alert-danger">{teamMemberError}</div>}
      <form className="row g-3 mb-4" onSubmit={onSubmit}>
        <div className="col-md-3">
          <label className="form-label">First Name</label>
          <input
            type="text"
            className="form-control"
            value={teamFirstName}
            onChange={(event) => setTeamFirstName(event.target.value)}
            required
          />
        </div>
        <div className="col-md-3">
          <label className="form-label">Last Name</label>
          <input
            type="text"
            className="form-control"
            value={teamLastName}
            onChange={(event) => setTeamLastName(event.target.value)}
            required
          />
        </div>
        <div className="col-md-3">
          <label className="form-label">Email</label>
          <input
            type="email"
            className="form-control"
            value={teamEmail}
            onChange={(event) => setTeamEmail(event.target.value)}
            required
          />
        </div>
        <div className="col-md-3">
          <label className="form-label">Phone</label>
          <input
            type="tel"
            className="form-control"
            value={teamPhone}
            onChange={(event) => setTeamPhone(event.target.value)}
            placeholder="Optional"
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">ServiceNow User Sys ID</label>
          <input
            type="text"
            className="form-control"
            value={teamSysId}
            onChange={(event) => setTeamSysId(event.target.value)}
            required
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">Geo</label>
          <select
            className="form-select"
            value={teamGeoId}
            onChange={(event) => setTeamGeoId(event.target.value)}
            required
          >
            <option value="">Select Geo</option>
            {geos.map((geo) => (
              <option key={geo.g_id} value={geo.g_id}>
                {geo.name}
              </option>
            ))}
          </select>
        </div>
        <div className="col-12 d-flex gap-2">
          <button type="submit" className="btn btn-primary" disabled={teamMemberSaving}>
            {teamMemberSaving ? 'Saving...' : editingTeamMemberId ? 'Update Team Member' : 'Add Team Member'}
          </button>
          {editingTeamMemberId && (
            <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <div className="table-responsive">
        <table className="table table-bordered align-middle mb-0 setup-inline-table">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>First Name</th>
              <th>Last Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Geo</th>
              <th>ServiceNow User Sys ID</th>
              <th style={{ width: '180px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {teamMembers.map((member) => (
              <tr key={member.tm_id}>
                <td>{member.tm_id}</td>
                <td>{member.f_name}</td>
                <td>{member.l_name}</td>
                <td>{member.email || '-'}</td>
                <td>{member.phone || '-'}</td>
                <td>{member.geo?.name || '-'}</td>
                <td>{member.sys_id || '-'}</td>
                <td className="d-flex gap-2">
                  <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => onEdit(member)}>
                    Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => onDelete(member.tm_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {teamMembers.length === 0 && (
              <tr>
                <td colSpan={8} className="text-center">
                  No team members yet. Add your first team member to continue.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function InlineGeoShiftMappingStep({
  geos,
  shifts,
  geoShiftMappings,
  mappingGeoId,
  mappingShiftId,
  editingGeoShiftMappingId,
  geoShiftError,
  geoShiftSaving,
  setMappingGeoId,
  setMappingShiftId,
  onSubmit,
  onEdit,
  onDelete,
  onCancel,
}) {
  return (
    <>
      {geoShiftError && <div className="alert alert-danger">{geoShiftError}</div>}
      <form className="row g-3 mb-4" onSubmit={onSubmit}>
        <div className="col-md-4">
          <label className="form-label">Geo</label>
          <select
            className="form-select"
            value={mappingGeoId}
            onChange={(event) => setMappingGeoId(event.target.value)}
            required
          >
            <option value="">Select Geo</option>
            {geos.map((geo) => (
              <option key={geo.g_id} value={geo.g_id}>
                {geo.name}
              </option>
            ))}
          </select>
        </div>
        <div className="col-md-4">
          <label className="form-label">Shift</label>
          <select
            className="form-select"
            value={mappingShiftId}
            onChange={(event) => setMappingShiftId(event.target.value)}
            required
          >
            <option value="">Select Shift</option>
            {shifts.map((shift) => (
              <option key={shift.s_id} value={shift.s_id}>
                {shift.name}
              </option>
            ))}
          </select>
        </div>
        <div className="col-12 d-flex gap-2">
          <button type="submit" className="btn btn-primary" disabled={geoShiftSaving}>
            {geoShiftSaving ? 'Saving...' : editingGeoShiftMappingId ? 'Update Mapping' : 'Add Mapping'}
          </button>
          {editingGeoShiftMappingId && (
            <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <div className="table-responsive">
        <table className="table table-bordered align-middle mb-0 setup-inline-table">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Geo</th>
              <th>Shift</th>
              <th style={{ width: '180px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {geoShiftMappings.map((mapping) => (
              <tr key={mapping.gsm_id}>
                <td>{mapping.gsm_id}</td>
                <td>{mapping.geo?.name || '-'}</td>
                <td>{mapping.shift?.name || '-'}</td>
                <td className="d-flex gap-2">
                  <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => onEdit(mapping)}>
                    Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => onDelete(mapping.gsm_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {geoShiftMappings.length === 0 && (
              <tr>
                <td colSpan={4} className="text-center">
                  No geo-shift mappings yet. Connect each geo to the shifts it supports.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function InlineCiUserMappingStep({
  configurationItems,
  teamMembers,
  ciUserMappings,
  mappingConfigurationItemId,
  mappingTeamMemberId,
  mappingSortOrder,
  editingCiUserMappingId,
  ciUserError,
  ciUserSaving,
  setMappingConfigurationItemId,
  setMappingTeamMemberId,
  setMappingSortOrder,
  onSubmit,
  onEdit,
  onDelete,
  onCancel,
}) {
  return (
    <>
      {ciUserError && <div className="alert alert-danger">{ciUserError}</div>}
      <form className="row g-3 mb-4" onSubmit={onSubmit}>
        <div className="col-md-4">
          <label className="form-label">Configuration Item</label>
          <select
            className="form-select"
            value={mappingConfigurationItemId}
            onChange={(event) => setMappingConfigurationItemId(event.target.value)}
            required
          >
            <option value="">Select CI</option>
            {configurationItems.map((item) => (
              <option key={item.ci_id} value={item.ci_id}>
                {item.name}
              </option>
            ))}
          </select>
        </div>
        <div className="col-md-4">
          <label className="form-label">Team Member</label>
          <select
            className="form-select"
            value={mappingTeamMemberId}
            onChange={(event) => setMappingTeamMemberId(event.target.value)}
            required
          >
            <option value="">Select Team Member</option>
            {teamMembers.map((member) => (
              <option key={member.tm_id} value={member.tm_id}>
                {member.f_name} {member.l_name}
              </option>
            ))}
          </select>
        </div>
        <div className="col-md-2">
          <label className="form-label">Sort Order</label>
          <input
            type="number"
            className="form-control"
            value={mappingSortOrder}
            onChange={(event) => setMappingSortOrder(event.target.value)}
          />
        </div>
        <div className="col-12 d-flex gap-2">
          <button type="submit" className="btn btn-primary" disabled={ciUserSaving}>
            {ciUserSaving ? 'Saving...' : editingCiUserMappingId ? 'Update Mapping' : 'Add Mapping'}
          </button>
          {editingCiUserMappingId && (
            <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <div className="table-responsive">
        <table className="table table-bordered align-middle mb-0 setup-inline-table">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Configuration Item</th>
              <th>Team Member</th>
              <th>Sort Order</th>
              <th style={{ width: '180px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {ciUserMappings.map((mapping) => (
              <tr key={mapping.mapping_id}>
                <td>{mapping.mapping_id}</td>
                <td>{mapping.configurationItem?.name || '-'}</td>
                <td>{mapping.teamMember?.f_name} {mapping.teamMember?.l_name}</td>
                <td>{mapping.sortOrder ?? '-'}</td>
                <td className="d-flex gap-2">
                  <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => onEdit(mapping)}>
                    Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => onDelete(mapping.mapping_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {ciUserMappings.length === 0 && (
              <tr>
                <td colSpan={5} className="text-center">
                  No CI-user mappings yet. Link each CI to the team members who can own it.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function GuidedExternalStep({ step, onRefresh }) {
  return (
    <div className="border rounded p-3 bg-light">
      <h5 className="mb-2">Guided Step</h5>
      <p className="text-muted mb-3">
        This step still uses the existing admin page so you can work with the current full-featured form.
        Open the page, complete your entries, then return here and refresh setup progress.
      </p>
      <div className="d-flex gap-2 flex-wrap">
        <Link className="btn btn-primary" to={`${step.route}?setup=1`}>
          {step.required ? `Open ${step.label}` : `Open Optional ${step.label}`}
        </Link>
        <button type="button" className="btn btn-outline-secondary" onClick={onRefresh}>
          Refresh Progress
        </button>
      </div>
    </div>
  );
}

function InlineServiceNowConnectionStep({
  serviceNowConfig,
  serviceNowInstanceUrl,
  serviceNowUsername,
  serviceNowPassword,
  serviceNowError,
  serviceNowSaving,
  setServiceNowInstanceUrl,
  setServiceNowUsername,
  setServiceNowPassword,
  onSubmit,
}) {
  return (
    <>
      <div className="alert alert-primary">
        Connect the organization’s ServiceNow instance first. Once this connection is verified, the remaining team setup steps unlock automatically.
      </div>
      {serviceNowConfig?.configured && (
        <div className="alert alert-success">
          ServiceNow is connected for this organization.
          {serviceNowConfig.connectedAt && (
            <> Last verified at {new Date(serviceNowConfig.connectedAt).toLocaleString()}.</>
          )}
        </div>
      )}
      {serviceNowError && <div className="alert alert-danger">{serviceNowError}</div>}
      <form className="row g-3" onSubmit={onSubmit}>
        <div className="col-md-12">
          <label className="form-label">ServiceNow Instance URL</label>
          <input
            type="url"
            className="form-control"
            value={serviceNowInstanceUrl}
            onChange={(event) => setServiceNowInstanceUrl(event.target.value)}
            placeholder="https://your-instance.service-now.com"
            required
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">ServiceNow Username</label>
          <input
            type="text"
            className="form-control"
            value={serviceNowUsername}
            onChange={(event) => setServiceNowUsername(event.target.value)}
            required
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">ServiceNow Password</label>
          <input
            type="password"
            className="form-control"
            value={serviceNowPassword}
            onChange={(event) => setServiceNowPassword(event.target.value)}
            placeholder={serviceNowConfig?.configured ? 'Re-enter to update credentials' : ''}
            required
          />
        </div>
        <div className="col-12 d-flex gap-2">
          <button type="submit" className="btn btn-primary" disabled={serviceNowSaving}>
            {serviceNowSaving ? 'Connecting...' : serviceNowConfig?.configured ? 'Update Connection' : 'Connect ServiceNow'}
          </button>
        </div>
      </form>
    </>
  );
}
